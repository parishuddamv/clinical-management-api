package com.clinicos.common.security;

import com.clinicos.common.entity.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that:
 * 1. Validates JWT tokens
 * 2. Extracts clinic_id from JWT claims
 * 3. Sets TenantContext for multi-tenancy
 * 4. Sets Spring Security authentication
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = getJwtFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.extractUsername(token);
                String clinicId = jwtTokenProvider.extractClinicId(token);

                // Set tenant context for current request
                TenantContext.setClinicId(clinicId);

                // Set Spring Security authentication with clinicId stored in details
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, null);
                authentication.setDetails(clinicId);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user: {} with clinic: {}", username, clinicId);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        } finally {
            // Clear TenantContext after processing
            // Note: In production, you might want to clear this in a finally block at the end of request
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

