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
