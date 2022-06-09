package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.payload.request.LoginRequest;
import com.example.javadoc0032022.payload.request.RegisterRequest;
import com.example.javadoc0032022.payload.response.JwtResponse;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.security.jwt.JwtUtils;
import com.example.javadoc0032022.security.service.UserDetailsImpl;
import com.example.javadoc0032022.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    static final long MIN1 = 60000;
    static final long MIN10 = 600000;
    static final long HOUR1 = 3600000;

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
//        final Optional<User> user = userService.findByLogin(loginRequest.getLogin());
//        if (user.isEmpty()) {
//            return new ResponseEntity<>("Invalid Username and Password", HttpStatus.UNAUTHORIZED);
//        }
//
//
//        if (passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword()) && user.get().getBlockTime() <= Calendar.getInstance().getTimeInMillis()) {
//            if (!user.get().isNonBlocked()) {
//                return new ResponseEntity<>(new MessageResponse("Locked"), HttpStatus.UNAUTHORIZED);
//            } else {
//                user.get().setLoginAttempts(0);
//                user.get().setNonBlocked(true);
//                userService.save(user.get());
                return authenticateUser(loginRequest.getLogin(), loginRequest.getPassword());
//            }
//
//        } else {
//
//
////            if (!user.get().isNonBlocked()) {
////                return new ResponseEntity<>(new MessageResponse("Account locked"), HttpStatus.UNAUTHORIZED);
////            }
//            int attempts = user.get().getLoginAttempts() + 1;
//            user.get().setLoginAttempts(attempts);
//            if (user.get().getLoginAttempts() > 3) {
//                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + MIN1);
//                user.get().setNonBlocked(false);
//            } else if (user.get().getLoginAttempts() > 4) {
//                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + MIN10);
//                user.get().setNonBlocked(false);
//            } else if (user.get().getLoginAttempts() > 5) {
//                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + HOUR1);
//                user.get().setNonBlocked(false);
//            }
//
//            userService.save(user.get());
//            return new ResponseEntity<>(new MessageResponse("Attempts " + user.get().getLoginAttempts()), HttpStatus.UNAUTHORIZED);
//        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByLogin(registerRequest.getLogin())) {
            return ResponseEntity.badRequest()
                    .body("User " + registerRequest.getLogin() + " already register");
        }

        if (registerRequest.getLogin().equals(registerRequest.getPassword())) {
            return ResponseEntity.badRequest()
                    .body("username must not match password");
        }

        User user = new User();
        user.setLogin(registerRequest.getLogin());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName());
        user.setLastName(registerRequest.getLastName());
        user.setSurName(registerRequest.getSurName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setNonBlocked(true);

        Set<Role> roleSet = new HashSet<>();
        Set<String> strRoles = registerRequest.getRole();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRole(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roleSet.add(userRole);
        } else {
            roleSet = userService.addRoles(strRoles);
        }

        user.setRoles(roleSet);
        userService.save(user);
        return authenticateUser(registerRequest.getLogin(), registerRequest.getPassword());
    }

    private ResponseEntity<?> authenticateUser(String login, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(userDetails.getId(),
                jwt,
                userDetails.getLogin(),
                roles,
                userDetails.getName(),
                userDetails.getLastName(),
                userDetails.getSurName(),
                userDetails.getEmail(),
                userDetails.getPhoneNumber(),
                userDetails.isNonBlocked())
        );
    }

}

