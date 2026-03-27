package com.example.ngerShop_be.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Các path filter sẽ KHÔNG xử lý JWT (public endpoints)
    // Dùng exact path cho /api/auth/* thay vì prefix để tránh bỏ sót /logout
    private final List<String> publicExactPaths = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/callback"
    );
    private final List<String> publicPrefixes = List.of(
            "/swagger/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/api-docs/",
            "/uploads/"
    );

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (publicExactPaths.contains(path)) return true;
        for (String prefix : publicPrefixes) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 1. Kiểm tra Header có chứa Bearer Token không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String subject = null;

        try {
            subject = jwtService.extractSubject(token);
        } catch (Exception ex) {
            // Nếu token lỗi (hết hạn, sai định dạng), cho đi tiếp để SecurityConfig chặn lại ở 401
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Kiểm tra xem User đã được xác thực chưa
        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(subject);
            } catch (UsernameNotFoundException ex) {
                filterChain.doFilter(request, response);
                return;
            }

            // --- ĐOẠN CẦN THÊM: Check Token có thực sự hợp lệ không ---
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
