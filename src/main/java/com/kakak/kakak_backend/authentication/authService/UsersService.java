package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.Config.JwtUtil;
import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import com.kakak.kakak_backend.authentication.authRepository.RoleRepo;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
@RequiredArgsConstructor
@Service
public class UsersService {
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private UsersRepo usersrepo;
        private final RoleRepo roleRepo;



        public Map<String, String> registeruser(AuthUsers user) {
            AuthRole role = roleRepo.findByName("WORKER")
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setPassword_hash(passwordEncoder.encode(user.getPassword_hash()));
            usersrepo.save(user);
            String accessToken = jwtUtil.GenerateToken(user.getEmail());
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);
            return response;

        }
}
