package com.example.ThirdProject.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.io.IOException;

public class RequestCachingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            // 모든 필터의 최상단에서 래퍼로 감싸버립니다.
            ContentCachingRequestWrapper wrappingRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
            chain.doFilter(wrappingRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}