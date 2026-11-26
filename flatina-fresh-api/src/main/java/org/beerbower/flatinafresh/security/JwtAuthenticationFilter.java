package org.beerbower.flatinafresh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.user.AppUserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Authenticates Bearer tokens. The user is looked up on each request, so removed users lose access at once. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.verify(header.substring(7))
                    .flatMap(users::findByUsernameIgnoreCase)
                    .ifPresent(user -> {
                        List<SimpleGrantedAuthority> roles = new ArrayList<>();
                        roles.add(new SimpleGrantedAuthority("ROLE_USER"));
                        if (user.isAdmin()) {
                            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user.getUsername(), null, roles));
                    });
        }
        chain.doFilter(request, response);
    }
}
