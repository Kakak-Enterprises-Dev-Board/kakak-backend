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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
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

            if (roleRepo.findByName("ADMIN").isEmpty()) {
                AuthRole admin = new AuthRole();
                admin.setName("ADMIN");
                admin.setDescription("System Administrator");
                roleRepo.save(admin);
            }

            if (roleRepo.findByName("EMPLOYER").isEmpty()) {
                AuthRole employer = new AuthRole();
                employer.setName("EMPLOYER");
                employer.setDescription("Employer User");
                roleRepo.save(employer);
            }

            if (roleRepo.findByName("WORKER").isEmpty()) {
                AuthRole worker = new AuthRole();
                worker.setName("WORKER");
                worker.setDescription("Worker User");
                roleRepo.save(worker);
            }
        };
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
