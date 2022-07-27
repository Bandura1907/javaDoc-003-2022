package com.example.javadoc0032022;

import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.MultipartConfigElement;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableAsync
public class JavaDoc0032022Application {

    public static void main(String[] args) {
        SpringApplication.run(JavaDoc0032022Application.class, args);
    }

//    @Bean
//    MultipartConfigElement multipartConfigElement() {
//        MultipartConfigFactory factory = new MultipartConfigFactory();
//        factory.setMaxFileSize(DataSize.ofMegabytes(30)); //10GB
//        factory.setMaxRequestSize(DataSize.ofMegabytes(30)); //10GB
//        return factory.createMultipartConfig();
//    }

    @Bean
    CommandLineRunner init(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (roleRepository.findByRole(ERole.ROLE_ADMIN).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
            }

            if (roleRepository.findByRole(ERole.ROLE_USER).isEmpty())
                roleRepository.save(new Role(ERole.ROLE_USER));

            if (roleRepository.findByRole(ERole.ROLE_EMPLOYEE).isEmpty())
                roleRepository.save(new Role(ERole.ROLE_EMPLOYEE));

            if (!userRepository.existsByLogin("adm")) {
                Set<Role> roleSet = new HashSet<>();
                roleSet.add(roleRepository.findByRole(ERole.ROLE_ADMIN).get());
                User user = new User();
                user.setLogin("adm");
                user.setPassword(encoder.encode("adm12"));
                user.setEmail("admin@mail.com");
                user.setRoles(roleSet);
                user.setEnabled(true);
                user.setNonBlocked(true);
                user.setNameOrganization("АО \"ОТП Банк\"");
                user.setName("admin");
                user.setLastName("admin");
                user.setSurName("admin");
                user.setIdentificationNumber("0000");
                user.setMainStateRegistrationNumber("0000");
                user.setSubdivision("admin");
                user.setPosition("admin");

                userRepository.save(user);
            }
        };
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("*")
                        .allowedHeaders("*")
                        .allowedOrigins("*");
            }
        };
    }
}
