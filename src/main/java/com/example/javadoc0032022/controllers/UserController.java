package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.payload.request.ChangePasswordRequest;
import com.example.javadoc0032022.payload.request.InfoUserRequest;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @PutMapping("/block/{id}")
    public ResponseEntity<MessageResponse> blockUser(@PathVariable int id) {
        return ResponseEntity.ok(new MessageResponse(userService.blockUser(id)));
    }

    @PutMapping("/change_password/{id}")
    public ResponseEntity<MessageResponse> changePassword(@PathVariable int id, @RequestBody @Valid ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(new MessageResponse(userService
                .changePassword(id, changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword())));
    }

    @PutMapping("/change_info/{id}")
    public ResponseEntity<User> changeUserInfo(@PathVariable int id, @RequestBody InfoUserRequest infoUserRequest) {
        return ResponseEntity.ok(userService.changeInfoUser(id, infoUserRequest));
    }
}
