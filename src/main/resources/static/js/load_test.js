import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,
    duration: '1m',
    thresholds: {
        http_req_duration: ['p(95)<200'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'https://api.xn--2y5bw4a.site';

export default function () {
    const userIndex = (__VU % 100) === 0 ? 100 : (__VU % 100);
    const username = `test_id_${userIndex}`;
    const password = 'password123';

    // 1. 로그인 (CSRF 필터가 /api/users/** 경로를 제외하므로 토큰 없이 시도 가능)
    const loginPayload = JSON.stringify({ username, password });
    const loginParams = { headers: { 'Content-Type': 'application/json' } };
    const loginRes = http.post(`${BASE_URL}/api/users/login`, loginPayload, loginParams);

    if (!check(loginRes, { 'Login Success': (r) => r.status === 200 })) return;

    // 2. 로그인 응답 쿠키에서 XSRF-TOKEN 추출
    // k6는 VU별 Jar에 쿠키를 저장하지만, 필터 검증을 위해 헤더에 직접 값을 박아줘야 합니다.
    const jar = http.cookieJar();
    const cookies = jar.cookiesForURL(BASE_URL);
    const csrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : null;

    // 3. 프로젝트 목록 조회 (GET 요청은 필터에서 통과되지만, 상세 조회나 CUD를 위해 헤더 구성법 숙지)
    const authParams = {
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': csrfToken, // 커스텀 필터가 요구하는 헤더 명칭
        },
    };

    const listRes = http.get(`${BASE_URL}/api/projects?page=0&size=10`, authParams);

    if (check(listRes, { 'Get Project List Success': (r) => r.status === 200 })) {
        const sliceResponse = listRes.json();

        if (sliceResponse.content && sliceResponse.content.length > 0) {
            const projectId = sliceResponse.content[0].id;

            // 4. 프로젝트 상세 조회
            const detailRes = http.get(`${BASE_URL}/api/projects/${projectId}`, authParams);

            check(detailRes, {
                'Project Detail Success (200)': (r) => r.status === 200,
            });
        }
    }

    sleep(Math.random() * 2 + 1);
}