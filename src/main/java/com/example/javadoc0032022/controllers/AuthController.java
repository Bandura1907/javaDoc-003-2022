package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.email.EmailSender;
import com.example.javadoc0032022.exception.TokenRefreshException;
import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.token.ConfirmationToken;
import com.example.javadoc0032022.models.token.RefreshToken;
import com.example.javadoc0032022.payload.request.LoginRequest;
import com.example.javadoc0032022.payload.request.RegisterRequest;
import com.example.javadoc0032022.payload.request.TokenRefreshRequest;
import com.example.javadoc0032022.payload.response.JwtResponse;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.payload.response.TokenRefreshResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.security.jwt.JwtUtils;
import com.example.javadoc0032022.security.service.RefreshTokenService;
import com.example.javadoc0032022.security.service.UserDetailsImpl;
import com.example.javadoc0032022.services.ConfirmationTokenService;
import com.example.javadoc0032022.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "AuthController", description = "Контролер управления авторизацией")
public class AuthController {
    static final long MIN1 = 60000;
    static final long MIN10 = 600000;
    static final long HOUR1 = 3600000;

    @Value("${doc.app.confirmation.token.link}")
    private String confirmationTokenLink;

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          UserService userService, PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository, RefreshTokenService refreshTokenService,
                          ConfirmationTokenService confirmationTokenService, EmailSender emailSender) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
        this.confirmationTokenService = confirmationTokenService;
        this.emailSender = emailSender;
    }

    @Operation(summary = "Метод для авторизации пользователя", description = "После 3 попыток неудачной авторизации блокирует юзера на 1 минуту," +
            "4 попытки - 10 минут, 5 и больше - 1 час")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Авторизирован",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизирован",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Юзер не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        final Optional<User> user = userService.findByLogin(loginRequest.getLogin());
        if (user.isEmpty()) {
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);
        }


        if (passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword()) && user.get().getBlockTime() <= Calendar.getInstance().getTimeInMillis()) {
            user.get().setLoginAttempts(0);
            user.get().setTimeLocked(false);
            userService.save(user.get());
            return authenticateUser(loginRequest.getLogin(), loginRequest.getPassword());

        } else {
            System.out.println(new Date(user.get().getBlockTime()));
            if (user.get().isTimeLocked() && Calendar.getInstance().getTimeInMillis() <= user.get().getBlockTime()) {
                return new ResponseEntity<>(new MessageResponse("time locke"), HttpStatus.UNAUTHORIZED);
            }

            int attempts = user.get().getLoginAttempts() + 1;
            user.get().setLoginAttempts(attempts);
            if (user.get().getLoginAttempts() == 4) {
                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + MIN1);
                user.get().setTimeLocked(true);
            } else if (user.get().getLoginAttempts() == 5) {
                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + MIN10);
                user.get().setTimeLocked(true);
            } else if (user.get().getLoginAttempts() >= 6) {
                user.get().setBlockTime(Calendar.getInstance().getTimeInMillis() + HOUR1);
                user.get().setTimeLocked(true);
            }

            userService.save(user.get());
            return new ResponseEntity<>(new MessageResponse("Attempts " + user.get().getLoginAttempts()), HttpStatus.UNAUTHORIZED);
        }
    }

    @Operation(summary = "Метод для регестрации пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Юзер зарегестрирован",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "Юзер с таким логином уже зарегестрирован или " +
                    "пароль с логином не должны совпадать",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByLogin(registerRequest.getLogin())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("User " + registerRequest.getLogin() + " already register"));
        }

        if (userService.existsByEmail(registerRequest.getEmail()))
            return ResponseEntity.badRequest().body(new MessageResponse("User email " +
                    registerRequest.getEmail() + " already exist"));

        if (registerRequest.getLogin().equals(registerRequest.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("username must not match password"));
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
        user.setTimeLocked(false);
        user.setFirstLogin(true);


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

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15),
                user
        );

        confirmationTokenService.saveConfirmationToken(confirmationToken);

//        String link = "http://194.58.96.68:39193/email/" + token;
        emailSender.send(registerRequest.getEmail(), userService.buildActivationEmail(
                registerRequest.getName() + " " + registerRequest.getLastName(),
                confirmationTokenLink + token
        ));

//        return authenticateUser(registerRequest.getLogin(), registerRequest.getPassword());
//        return ResponseEntity.ok(new MessageResponse("You must be active email"));
        return ResponseEntity.ok(Map.of("id", user.getId(), "message", "You must be active email"));
    }

    @Operation(summary = "Метод обновления токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен обновлен",
                    content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class)))
    })
    @PostMapping("/refresh_token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateTokenFromUsername(user.getLogin());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database!"));
    }

    @Operation(summary = "Метод для потдверждения почти при регестрации")
    @ApiResponse(responseCode = "200", description = "Почта подтверждена",
            content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    @GetMapping("/confirm")
    public ResponseEntity<MessageResponse> confirm(@RequestParam("token") String token) {
        return ResponseEntity.ok(new MessageResponse(userService.confirmToken(token)));
    }

    private ResponseEntity<JwtResponse> authenticateUser(String login, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        return ResponseEntity.ok(new JwtResponse(userDetails.getId(),
                jwt,
                userDetails.getLogin(),
                roles,
                refreshToken.getToken(),
                userDetails.getName(),
                userDetails.getLastName(),
                userDetails.getSurName(),
                userDetails.getEmail(),
                userDetails.getPhoneNumber(),
                userDetails.isNonBlocked(),
                userDetails.isFirstLogin())
        );
    }

}

