package com.cookify.config;

import com.cookify.security.CookifyUserDetailsService;
import com.cookify.security.RestAccessDeniedHandler;
import com.cookify.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.remember-me.key:cookify-dev-remember-me-key-change-in-production}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CookifyUserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Persistent "remember me" cookie backing "session remembered" (test
     * case 2). JdbcTokenRepositoryImpl.setCreateTableOnStartup(true) runs
     * an unconditional CREATE TABLE with no IF NOT EXISTS -- it would
     * crash the app on every restart after the first against a
     * persisted database. Create the identical table ourselves,
     * idempotently, instead.
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        new JdbcTemplate(dataSource).execute(
                "create table if not exists persistent_logins (username varchar(64) not null, "
                        + "series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(PersistentTokenRepository tokenRepository,
                                                                       CookifyUserDetailsService userDetailsService) {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(rememberMeKey, userDetailsService, tokenRepository);
        services.setTokenValiditySeconds(60 * 60 * 24 * 30); // 30 days
        return services;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            SecurityContextRepository securityContextRepository,
                                            PersistentTokenBasedRememberMeServices rememberMeServices,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Plain (non-XOR) handler: the frontend reads the XSRF-TOKEN cookie
                        // verbatim and echoes it in the X-XSRF-TOKEN header -- the default
                        // XOR handler expects a masked value a vanilla-JS client can't produce.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/csrf", "/h2-console/**",
                                "/", "/index.html", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/**", "/api/users/**").permitAll()
                        .anyRequest().authenticated())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
