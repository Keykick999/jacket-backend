import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 부하 설정: 100명이 동시에 접속하여 1분 동안 지속
export const options = {
    vus: 100,               // 100명의 가상 유저 고정
    duration: '1m',         // 1분 동안 반복 실행
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청이 0.5초 이내여야 함
        http_req_failed: ['rate<0.01'],   // 에러율 1% 미만 유지
    },
};

const BASE_URL = 'https://api.xn--2y5bw4a.site';

export default function () {
    // 2. 가상 유저(VU)마다 고유한 계정 할당
    // __VU는 1부터 시작하는 k6 고유 ID입니다.
    // test_id_1, test_id_2 ... test_id_100 까지 중복 없이 할당됨
    const userIndex = (__VU % 100) === 0 ? 100 : (__VU % 100);
    const username = `test_id_${userIndex}`;
    const password = 'password123';

    // 3. 로그인 (인증 쿠키 및 CSRF 토큰 획득)
    const loginPayload = JSON.stringify({
        username: username,
        password: password,
    });

    const loginParams = {
        headers: { 'Content-Type': 'application/json' },
    };

    const loginRes = http.post(`${BASE_URL}/api/users/login`, loginPayload, loginParams);

    const isLoginOk = check(loginRes, {
        'Login Success (200)': (r) => r.status === 200,
    });

    if (!isLoginOk) {
        console.error(`[VU ${__VU}] 로그인 실패: ${username} (Status: ${loginRes.status})`);
        return;
    }

    // 4. 프로젝트 목록 조회 (본인이 소유한 프로젝트 ID를 동적으로 획득)
    const listRes = http.get(`${BASE_URL}/api/projects`);

    const isListOk = check(listRes, {
        'Get Project List Success': (r) => r.status === 200,
    });

    if (isListOk) {
        const projects = listRes.json();

        if (projects && projects.length > 0) {
            // 본인 소유 프로젝트 중 첫 번째 ID 조회
            const projectId = projects[0].id;

            // 5. 프로젝트 상세 조회 (본인 검증 로직이 포함된 핵심 API)
            const detailRes = http.get(`${BASE_URL}/api/projects/${projectId}`);

            check(detailRes, {
                'Project Detail Success (200)': (r) => r.status === 200,
                'Verify Ownership Match': (r) => r.json().id === projectId,
            });
        }
    }

    // 실제 사용자와 유사하게 1~3초 사이의 랜덤한 대기 시간 추가
    sleep(Math.random() * 2 + 1);
}