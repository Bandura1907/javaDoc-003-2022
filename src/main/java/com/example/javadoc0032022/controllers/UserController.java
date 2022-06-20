package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.email.EmailSender;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.payload.request.ChangePasswordRequest;
import com.example.javadoc0032022.payload.request.InfoUserRequest;
import com.example.javadoc0032022.payload.request.ResetPasswordRequest;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.payload.response.UserResponse;
import com.example.javadoc0032022.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
@Tag(name = "UserController", description = "Контролер управления юзерами (Методы доступны только после авторизации)")
public class UserController {

    private UserService userService;
    private EmailSender emailSender;
    private PasswordEncoder encoder;

    @Operation(summary = "Метод получения всех юзеров")
    @ApiResponse(responseCode = "200", description = "Получены все юзеры")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Метод получения юзера по id")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@Parameter(description = "ID user", required = true) @PathVariable int id) {
        return ResponseEntity.ok(userService.findByIdUserResponse(id));
    }

    @Operation(summary = "Метод для востановления пароля")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return Token"),
            @ApiResponse(responseCode = "404", description = "Email not found")
    })
    @PostMapping("/forgot_password")
    public ResponseEntity<MessageResponse> forgotPassword(@Parameter(description = "Можно передать в параметр или впихнуть в FormData", required = true)@RequestParam String email) {
        String response = userService.forgotPassword(email);

        if (!response.startsWith("Invalid")) {
            emailSender.send(email, userService.buildResetPasswordEmail(response, "http://194.58.96.68:39193/password/" + response));
            return ResponseEntity.ok(new MessageResponse(response));
        } else return new ResponseEntity<>(new MessageResponse(response), HttpStatus.NOT_FOUND);

    }

    @Operation(summary = "Метод для сброса пароля")
    @PutMapping("/reset_password")
    public ResponseEntity<MessageResponse> resetPassword(@Parameter(description = "Можно передать в параметр или впихнуть в FormData", required = true)@RequestParam String token,
                                                         @Parameter(description = "Можно передать в параметр или впихнуть в FormData", required = true)@RequestParam String password) {

        return ResponseEntity.ok(new MessageResponse(userService.resetPassword(token, password)));
    }

    @Operation(summary = "Метод блокировки юзера (Доступен только админу)", description = "Блокирование пользователя по ID. " +
            "Если юзер забанен - метод разблокирует юзера. Если юзер разблокирован - метод забанет юзера.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операция прошла успешно",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "500", description = "Юзер не найдет",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ только админу")
    })
    @PutMapping("/block/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> blockUser(@Parameter(description = "id user", required = true) @PathVariable int id) {
        return ResponseEntity.ok(new MessageResponse(userService.blockUser(id)));
    }

    @Operation(summary = "Метод изменения пароля", description = "Для изменеия пароля нужно сначала ввести старый а потом новый." +
            " Минимальная длина пароля 12")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно поменян или пароли не совпадают",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Юзер не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    @PutMapping("/change_password/{id}")
    public ResponseEntity<MessageResponse> changePassword(@Parameter(description = "id user", required = true) @PathVariable int id,
                                                          @Parameter(description = "Минимальная длина 12", required = true) @RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        try {
            String result = userService
                    .changePassword(id, changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());
            return ResponseEntity.ok(new MessageResponse(result));
        } catch (Exception e) {
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Редактирование информации юзера")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Инфа отредактирована",
                    content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Юзер с таким логином уже существует",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    @PutMapping("/change_info/{id}")
    public ResponseEntity<?> changeUserInfo(@Parameter(description = "id user", required = true) @PathVariable int id,
                                            @Parameter(description = "Можно менять одно или несколько полей", required = true) @Valid @RequestBody InfoUserRequest infoUserRequest) {
        try {
            User user = userService.changeInfoUser(id, infoUserRequest);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

    }

    @Operation(summary = "Метод для смены пароля при первом входе (Пока оставлю может пригодится) (Можно поменять в change_info (поле FirstLogin))")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/first_login_password_change/{id}")
    public ResponseEntity<MessageResponse> firstLoginPasswordChange(@Parameter(required = true, description = "User ID") @PathVariable int id,
                                                                    @Parameter(description = "ResetPasswordRequest", required = true) @RequestBody ResetPasswordRequest request) {
        Optional<User> user = userService.findById(id);
        if (user.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);

        user.get().setPassword(encoder.encode(request.getNewPassword()));
        user.get().setFirstLogin(false);
        userService.save(user.get());
        return ResponseEntity.ok(new MessageResponse("Password changed"));
    }
}
