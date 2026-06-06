package com.kakak.kakak_backend.Config;

import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import com.kakak.kakak_backend.authentication.authRepository.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                                ).permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    CommandLineRunner initRoles(RoleRepo roleRepo) {
        return args -> {
            java.util.List<AuthRole> allRoles = roleRepo.findAll();

            boolean hasAdmin = allRoles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));
            if (!hasAdmin) {
                AuthRole admin = new AuthRole();
                admin.setName("ADMIN");
                admin.setDescription("System Administrator");
                roleRepo.save(admin);
            }

            boolean hasEmployer = allRoles.stream().anyMatch(r -> "EMPLOYER".equalsIgnoreCase(r.getName()));
            if (!hasEmployer) {
                AuthRole employer = new AuthRole();
                employer.setName("EMPLOYER");
                employer.setDescription("Employer User");
                roleRepo.save(employer);
            }

            boolean hasWorker = allRoles.stream().anyMatch(r -> "WORKER".equalsIgnoreCase(r.getName()));
            if (!hasWorker) {
                AuthRole worker = new AuthRole();
                worker.setName("WORKER");
                worker.setDescription("Worker User");
                roleRepo.save(worker);
            }
        };
    }





    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
