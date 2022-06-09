package com.example.javadoc0032022;

import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JavaDoc0032022Application {

    public static void main(String[] args) {
        SpringApplication.run(JavaDoc0032022Application.class, args);
    }

    @Bean
    CommandLineRunner init(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByRole(ERole.ROLE_ADMIN).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
            }

            if (roleRepository.findByRole(ERole.ROLE_USER).isEmpty())
                roleRepository.save(new Role(ERole.ROLE_USER));

            if (roleRepository.findByRole(ERole.ROLE_EMPLOYEE).isEmpty())
                roleRepository.save(new Role(ERole.ROLE_EMPLOYEE));
        };
    }
}
