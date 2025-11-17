document.getElementById("loginBtn").addEventListener("click", async () => {
    const userId = document.getElementById("userIdInput").value;
    const password = document.getElementById("passwordInput").value;

    if (!userId || !password) {
        alert("아이디와 비밀번호를 모두 입력하세요.");
        return;
    }

    try {
        const response = await fetch("/users/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ userId, password }) // 서버에서 email 대신 userId 사용 시
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || "로그인 실패");
        }

        const data = await response.json();
        localStorage.setItem("token", data.token);

        alert("로그인 성공!");
        window.location.href = "/main";
    } catch (err) {
        console.error(err);
        alert("로그인 실패: " + err.message);
    }
});