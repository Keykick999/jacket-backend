import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,
    duration: '1m',
    thresholds: {
        http_req_duration: ['p(95)<200'], // 성능 개선 후이므로 목표치를 200ms로 낮춰보세요!
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'https://api.xn--2y5bw4a.site';

export default function () {
    const userIndex = (__VU % 100) === 0 ? 100 : (__VU % 100);
    const username = `test_id_${userIndex}`;
    const password = 'password123';

    // 1. 로그인
    const loginPayload = JSON.stringify({ username, password });
    const loginParams = { headers: { 'Content-Type': 'application/json' } };
    const loginRes = http.post(`${BASE_URL}/api/users/login`, loginPayload, loginParams);

    if (!check(loginRes, { 'Login Success': (r) => r.status === 200 })) return;

    // 2. 프로젝트 목록 조회 (Slice 대응)
    // 실제 서비스처럼 첫 페이지 10개를 요청합니다.
    const listRes = http.get(`${BASE_URL}/api/projects?page=0&size=10`);

    const isListOk = check(listRes, {
        'Get Project List Success': (r) => r.status === 200,
    });

    if (isListOk) {
        const sliceResponse = listRes.json(); // 이제 [{...}] 가 아니라 {"content": [...]} 임

        // [수정 핵심] .content 가 존재하는지, 그리고 그 안에 데이터가 있는지 확인
        if (sliceResponse.content && sliceResponse.content.length > 0) {
            const projectId = sliceResponse.content[0].id;

            // 3. 프로젝트 상세 조회
            const detailRes = http.get(`${BASE_URL}/api/projects/${projectId}`);

            check(detailRes, {
                'Project Detail Success (200)': (r) => r.status === 200,
                'Verify Ownership Match': (r) => r.json().id === projectId,
            });
        }
    }

    sleep(Math.random() * 2 + 1);
}