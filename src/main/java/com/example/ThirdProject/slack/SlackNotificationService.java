package com.example.ThirdProject.slack;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.util.HashMap;
import java.util.Map;

@Component
public class SlackNotificationService {

    private final String slackWebhookUrl; // 이제 final 가능!
    private final RestTemplate restTemplate;

    public SlackNotificationService(@Value("${slack.webhook.url}") String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
        this.restTemplate = new RestTemplate(); // 또는 Bean으로 등록된 걸 받아와도 됨
    }

    public void sendErrorNotification(String traceId, String userId, Exception e, HttpServletRequest request) {
        Map<String, Object> payload = new HashMap<>();
        String body = getBody(request);

        String errorMessage = String.format(
                "*[서버 에러 발생]* \n\n" +
                        "*Trace ID:* %s\n" +
                        "*User ID:* %s\n" +
                        "*URL:* %s %s\n" +
                        "*Error:* %s\n" +
                        "*Location:* %s:%d\n\n" +
                        "*Request Body:*\n```%s```",
                traceId, userId, request.getMethod(), request.getRequestURI(),
                e.getMessage(),
                e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(),
                body
        );

        payload.put("text", errorMessage);

        try {
            restTemplate.postForEntity(slackWebhookUrl, payload, String.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf);
            }
        }
        return "Empty Body";
    }
}