package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.authentication.authRepository.RoleRepo;
import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class RoleService {
         @Autowired
         private RoleRepo rolerepo;

         public void saverole(AuthRole role) {
             rolerepo.save(role);
         }
}
