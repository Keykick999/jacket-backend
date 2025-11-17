package com.example.ThirdProject.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class HomeController {

    @GetMapping("/")
    public String login() {
        return "/html/login";
    }

    @GetMapping("/signUp")
    public String signUp() {
        return "html/signUp";
    }

    /**
     * 1. OAuth2 로그인 성공 시 콜백
     * (SuccessHandler가 이쪽으로 리다이렉트시킴)
     */
    @GetMapping("/oauth/callback")
    public String oauthCallback(@RequestParam(value = "accessToken", required = false) String accessToken, Model model) {

        // 1. 토큰을 View(HTML)로 전달
        model.addAttribute("accessToken", accessToken);

        // 2. 이 View(oauth-callback.html)는 localStorage에 토큰을 저장하는
        //    JavaScript 코드를 포함해야 합니다.
        return "html/oauth-callback"; // (새로 만들어야 할 HTML 파일)
    }

    /**
     * 2. OAuth2 로그인 실패 시 콜백
     * (FailureHandler가 이쪽으로 리다이렉트시킴)
     */
    @GetMapping("/oauth/error")
    public String oauthError(@RequestParam(value = "error", required = false) String error, Model model) {

        // 1. 에러 코드를 View(HTML)로 전달
        model.addAttribute("error", error);

        // 2. 에러 코드를 사용자에게 보여주는 페이지
        return "html/oauth-error"; // (새로 만들어야 할 HTML 파일)
    }

    @GetMapping("/signUp/additional-info")
    public String addtionalSignUp(@RequestParam("providerId") String providerId, @RequestParam("provider") String provider, @RequestParam("nickname") String nickname, Model model) {
        model.addAttribute("providerId", providerId);
        model.addAttribute("provider", provider);
        model.addAttribute("nickname", nickname);
        return "html/additional-signup";
    }

    /**
     * [신규] 기존 계정 발견 시, 소셜 계정 연동 여부를 사용자에게 확인하는 페이지
     */
    @GetMapping("/link-confirm")
    public String linkConfirm(@RequestParam("providerId") String providerId,
                              @RequestParam("provider") String provider,
                              @RequestParam(value = "existingEmail", required = false) String existingEmail,
                              Model model) {

        // Model에 FailureHandler에서 전달받은 소셜 정보를 담아 View로 전달합니다.
        model.addAttribute("providerId", providerId);
        model.addAttribute("provider", provider);
        model.addAttribute("existingEmail", existingEmail);

        return "html/link-confirm";
    }
}