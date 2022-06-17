package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.exception.TokenRefreshException;
import com.example.javadoc0032022.models.*;
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
import com.example.javadoc0032022.services.EmailService;
import com.example.javadoc0032022.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "AuthController", description = "Контролер управления авторизацией")
public class AuthController {
    static final long MIN1 = 60000;
    static final long MIN10 = 600000;
    static final long HOUR1 = 3600000;

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private RefreshTokenService refreshTokenService;
    private EmailService emailService;
    private ConfirmationTokenService confirmationTokenService;


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

        if (registerRequest.getLogin().equals(registerRequest.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("username must not match password"));
        }

        String token = UUID.randomUUID().toString();

        User user = new User();
        user.setLogin(registerRequest.getLogin());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName());
        user.setLastName(registerRequest.getLastName());
        user.setSurName(registerRequest.getSurName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
//        user.setNonBlocked(true);
        user.setTimeLocked(false);

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
        userService.saveConfirmationToken(user, token);
        String link = "http://localhost:3001/api/auth/confirm?token=" + token;
        emailService.sendEmail(registerRequest.getEmail(), buildEmail(registerRequest.getName(), link));

        return ResponseEntity.ok(new MessageResponse("you must confirm your email"));
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
                userDetails.isNonBlocked())
        );
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

    @GetMapping("/confirm")
    public ResponseEntity<MessageResponse> confirm(@RequestParam("token") String token) {
        Optional<ConfirmationToken> confirmationToken = confirmationTokenService.getToken(token);

        if (confirmationToken.isEmpty()) {
            throw new IllegalStateException("Token not found!");
        }

        if (confirmationToken.get().getConfirmedAt() != null) {
            throw new IllegalStateException("Email is already confirmed");
        }

        LocalDateTime expiresAt = confirmationToken.get().getExpiresAt();

        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token is already expired!");
        }

        confirmationTokenService.setConfirmedAt(token);
        userService.enableAppUser(confirmationToken.get().getUser().getEmail());

        //Returning confirmation message if the token matches
        return ResponseEntity.ok(new MessageResponse("Your email is confirmed. Thank you for using our service!"));
    }

    private String buildEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering. Please click on the below link to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

}

