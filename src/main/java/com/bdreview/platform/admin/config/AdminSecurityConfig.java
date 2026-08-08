package com.bdreview.platform.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Session/form-login security for the server-rendered Thymeleaf admin panel
 * (mounted at /admin — spec §12 "moderation staff" surface).
 *
 * <p>This is intentionally a completely separate {@link SecurityFilterChain}
 * from the one in {@code auth.SecurityConfig}: the rest of the application is
 * a stateless JWT API (bearer tokens, no session, no CSRF) and stays exactly
 * as it was — nothing here touches that class. This chain is scoped with
 * {@code securityMatcher("/admin/**")} and given {@code @Order(1)} so it is
 * evaluated first for admin-panel URLs; every other request falls through
 * unchanged to the existing (unordered, lowest-precedence) JWT chain.
 */
@Configuration
public class AdminSecurityConfig {

    private final AdminAuthenticationProvider adminAuthenticationProvider;

    public AdminSecurityConfig(AdminAuthenticationProvider adminAuthenticationProvider) {
        this.adminAuthenticationProvider = adminAuthenticationProvider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .authenticationProvider(adminAuthenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/css/**", "/admin/js/**").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("phoneNumber")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll())
                .exceptionHandling(ex -> ex.accessDeniedPage("/admin/login?denied"));
        return http.build();
    }
}
