package za.co.taloms.security.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import za.co.taloms.company.presentation.CompanyApiKeyAuthenticationFilter;
import za.co.taloms.security.application.service.UserDetailsServiceImpl;
import za.co.taloms.security.presentation.LoginFailureHandler;
import za.co.taloms.security.presentation.LoginSuccessHandler;
import za.co.taloms.security.presentation.JwtAuthenticationFilter;
import za.co.taloms.security.presentation.TalomsAccessDeniedHandler;
import za.co.taloms.security.presentation.TalomsAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter         jwtAuthFilter;
    private final UserDetailsServiceImpl         userDetailsService;
    private final LoginSuccessHandler            loginSuccessHandler;
    private final LoginFailureHandler            loginFailureHandler;
    private final TalomsAuthenticationEntryPoint entryPoint;
    private final TalomsAccessDeniedHandler      accessDeniedHandler;
    private final CompanyApiKeyAuthenticationFilter companyApiKeyFilter;
    private final za.co.taloms.common.resiliency.ApiProtectionFilter apiProtectionFilter;

    @org.springframework.beans.factory.annotation.Value("${taloms.remember-me.key}")
    private String rememberMeKey;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/register/**",
                                "/forgot-password",
                                "/reset-password",
                                "/api/auth/**",
                                "/api/users/forgot-password",
                                "/api/users/reset-password",
                                "/ptos/villages/**",
                                "/parcels/villages/**",
                                "/actuator/health",
                                "/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/icons/**",
                                "/sw.js",
                                "/manifest.webmanifest",
                                "/offline.html",
                                                                "/favicon.ico",
                                "/error",
                                "/error/**",
                                "/.well-known/**"
                        ).permitAll()
                        // Change password - accessible to all authenticated users
                        .requestMatchers("/users/change-password").authenticated()
                        // Admin-only pages
                        .requestMatchers("/users", "/users/**").hasRole("ADMIN")
                        .requestMatchers("/audit", "/audit/**").hasRole("ADMIN")
                        // User search for leadership selection - ADMIN and CHIEF both use it
                        .requestMatchers("/api/users/search").hasAnyRole("ADMIN", "CHIEF")
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit", "/api/audit/**").hasRole("ADMIN")
                        // Authorities - ADMIN creates/manages authorities; CHIEF adds villages
                        .requestMatchers("/authorities", "/authorities/**").hasAnyRole("ADMIN", "CHIEF")
                        .requestMatchers("/api/authorities", "/api/authorities/**").hasAnyRole("ADMIN", "CHIEF")
                        // Villages - CHIEF-only (admin cannot view or manage villages)
                        .requestMatchers("/villages", "/villages/**").hasRole("CHIEF")
                        .requestMatchers("/api/villages", "/api/villages/**").hasRole("CHIEF")
                        // All authenticated users (read-only dashboard for every role)
                        .requestMatchers("/dashboard", "/api/dashboard/**").authenticated()
                        // Company self-service (ROLE_COMPANY can view/manage their own account)
                        .requestMatchers("/company", "/company/**").hasRole("COMPANY")
                        .requestMatchers("/api/company", "/api/company/**").hasRole("COMPANY")
                        // Resident portal - ROLE_USER self-service
                        .requestMatchers("/portal", "/portal/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/portal/**").hasAnyRole("USER", "ADMIN")
                        // Staff-only operational pages - ROLE_USER cannot read/modify PTOs, parcels, etc.
                        .requestMatchers("/ptos/**", "/parcels/**", "/gis/**", "/documents/**")
                                .hasAnyRole("ADMIN", "CHIEF", "HEADSMAN")
                        .requestMatchers("/api/ptos/**", "/api/parcels/**", "/api/gis/**", "/api/documents/**")
                                .hasAnyRole("ADMIN", "CHIEF", "HEADSMAN")
                        // ── External company API ───────────────────────────────────────
                        // Dedicated API-key authentication (CompanyApiKeyAuthenticationFilter).
                        // A COMPANY authority is only ever granted by that filter, so a
                        // browser/JWT session can never reach these endpoints.
                        .requestMatchers("/api/external/**").hasRole("COMPANY")
                        // ── Company self-service (ROLE_COMPANY browser login) ─────────────
                        // Companies that log in via form authentication can manage their own
                        // API keys and view usage statistics here.
                        .requestMatchers("/company", "/company/**").hasRole("COMPANY")
                        .requestMatchers("/api/company/**").hasRole("COMPANY")
                        // ── Company administration ─────────────────────────────────────
                        // Companies must never reach these: ADMIN only.
                        .requestMatchers("/companies", "/companies/**").hasRole("ADMIN")
                        .requestMatchers("/api/companies", "/api/companies/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(604800)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .authenticationProvider(authenticationProvider())
                .addFilterAfter(
                        companyApiKeyFilter,
                        SecurityContextHolderFilter.class
                )
                .addFilterAfter(
                        apiProtectionFilter,
                        SecurityContextHolderFilter.class
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

