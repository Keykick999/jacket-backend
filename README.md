# Jacket (backend)

웹 브라우저용 DAW Jacket의 백엔드입니다.
음악 프로젝트·트랙·앨범 API와 함께, CPU 장애를 Slack으로 분석해 보내는 **incident-analyzer**와 로컬 모니터링 스택이 들어 있습니다.

프론트엔드: [jacket-frontend](https://github.com/Keykick999/jacket-frontend)

| 구성 | 설명 | 포트 |
| --- | --- | --- |
| Jacket | 서비스 본체 (`src/`) | 8080 |
| incident-analyzer | Alert 수신 → Rule + Gemini 분석 → Slack | 8081 |
| Prometheus | Jacket actuator 지표 수집, `JacketHighCPU` 평가 | 9090 |
| Alertmanager | firing alert를 analyzer webhook으로 전달 | 9093 |
| Grafana | 대시보드 (기본 계정 `admin` / `admin`) | 3001 |

Java 17, Gradle Wrapper가 필요합니다. incident-analyzer는 Spring Boot 4.1.1 + Spring AI 2.0.1입니다.

## 장애 분석 흐름

```text
Jacket :8080
  actuator/prometheus
        │
        ▼
Prometheus :9090  ──JacketHighCPU──►  Alertmanager :9093
                                              │
                                              ▼ POST /api/incidents/alerts
                                      incident-analyzer :8081
                                              │
                         ┌────────────────────┴────────────────────┐
                         ▼                                         ▼
                  Rule (JFR CPU 등)                         Gemini (추론)
                         │                                         │
                         └──────── Slack (Rule은 항상, LLM은 성공 시) ─┘
```

1. Prometheus가 `host.docker.internal:8080/actuator/prometheus`를 수집합니다.
2. `JacketHighCPU`가 firing되면 Alertmanager가 `http://host.docker.internal:8081/api/incidents/alerts`로 보냅니다.
3. analyzer는 Rule 기반 정량 분석(현재 High CPU → JFR)을 먼저 수행합니다.
4. Gemini가 추가 증거가 필요하면 허용된 도구만 JSON으로 요청합니다. 도구 실행은 서버가 합니다.
5. Slack에는 Rule 결과와 LLM 추론을 구분해 보냅니다. LLM이 실패해도 Rule 알림은 나갑니다.

허용 도구:

- `query_metrics` — Prometheus 구간 조회 (`process_cpu_usage`, `system_cpu_usage` 등)
- `analyze_jfr` — 설정된 JFR 파일의 상위 CPU 메서드
- `collect_thread_dump` — Jacket `actuator/threaddump`

로그 저장소가 없어 `search_logs`는 없습니다. 경로·셸 도구도 LLM에 열려 있지 않습니다.

같은 fingerprint + `startsAt` 조합은 캐시 TTL(기본 10분) 동안 다시 분석하지 않습니다. Alertmanager가 이미 firing 중인 알림도 재전송하지 않으므로, 로컬에서 다시 보려면 모니터링 스택을 내렸다 올리거나 analyzer에 테스트 webhook을 직접 치면 됩니다.

## 로컬 실행

Windows PowerShell 기준입니다. Docker Desktop이 켜져 있어야 Prometheus / Alertmanager / Grafana가 뜹니다.

### 1. Jacket

`src/main/resources/application.properties`는 git에 없습니다. 로컬에 MySQL, Redis, MinIO 등 연결 정보를 넣은 뒤:

```powershell
.\gradlew bootRun
```

확인:

- http://localhost:8080
- http://localhost:8080/actuator/prometheus
- http://localhost:8080/actuator/threaddump

CPU 부하 재현: `GET /test/cpu`

### 2. 모니터링

```powershell
cd monitoring
docker compose up -d
```

- Prometheus: http://localhost:9090
- Alertmanager: http://localhost:9093
- Grafana: http://localhost:3001

### 3. incident-analyzer

API 키와 Slack webhook은 소스에 넣지 않습니다. Windows 사용자 환경 변수 또는 세션 환경 변수로 넘깁니다.

```powershell
$env:GEMINI_API_KEY = "<your-key>"            # LLM 사용 시 필수
$env:SLACK_WEBHOOK_URL = "<incoming-webhook>"  # 없으면 Slack을 건너뜀
$env:INCIDENT_JFR_DIRECTORY = "C:\Users\<you>\jacket-backend"
cd incident-analyzer
.\gradlew bootRun
```

선택 환경 변수:

| 변수 | 기본값 | 의미 |
| --- | --- | --- |
| `GEMINI_MODEL` | `gemini-3.8-flash` | Spring AI Google GenAI 모델 |
| `INCIDENT_LLM_ENABLED` | `true` | LLM 분석 on/off |
| `PROMETHEUS_BASE_URL` | `http://localhost:9090` | 지표 조회 |
| `INCIDENT_JFR_DIRECTORY` | analyzer 작업 디렉터리 | `jacket-cpu-test.jfr`가 있는 폴더 |
| `INCIDENT_THREADDUMP_URL` | `http://localhost:8080/actuator/threaddump` | 스레드 덤프 |

JFR 파일 이름은 `incident.jfr.file-name`(기본 `jacket-cpu-test.jfr`)입니다.

## Slack 메시지

```text
[Alert]
Alert / Severity / Instance

[Rule 기반 분석 (정량)]
JFR 상위 메서드 등 정량 결과

[LLM 분석 (추론, 검증 필요)]
요약, 원인 후보, 권장 대응, 한계, Rule 대비
```

LLM은 제공된 증거 ID만 근거로 쓰고, Rule과 판단이 다르면 `Rule 대비`에 적습니다. 호출이 실패하면 상태와 사유만 붙고 Rule 섹션은 그대로 갑니다.

## 테스트

Jacket:

```powershell
.\gradlew test
```

incident-analyzer (실제 Gemini 호출 제외):

```powershell
cd incident-analyzer
.\gradlew test
```

실제 Gemini 호출 (`GEMINI_API_KEY` 필요):

```powershell
cd incident-analyzer
.\gradlew geminiTest
```

## 현재 Alert 규칙

`monitoring/alert-rules.yml`의 `JacketHighCPU`는 로컬 검증용으로 `process_cpu_usage >= 0`을 10초 유지하면 firing합니다. annotation의 “80%”와 실제 식은 아직 다릅니다. 운영에서는 임계값을 올리고, 호스트 CPU(`system_cpu_usage`)와 프로세스 CPU를 분리하는 편이 맞습니다.

## 배포

`main` 푸시 시 `.github/workflows/deploy.yml`이 Jacket을 빌드해 Docker Hub에 올리고 GCE에 배포합니다. `APPLICATION_PROPERTIES` 등 비밀값은 GitHub Secrets로만 넣습니다. incident-analyzer와 모니터링 스택은 이 워크플로에 포함되어 있지 않습니다.