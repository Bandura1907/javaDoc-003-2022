package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.ERole;
import com.example.javadoc0032022.models.token.ConfirmationToken;
import com.example.javadoc0032022.models.token.ResetToken;
import com.example.javadoc0032022.payload.request.InfoUserRequest;
import com.example.javadoc0032022.payload.response.UserResponse;
import com.example.javadoc0032022.repository.ResetTokenRepository;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class UserService {

    private static final long EXPIRE_TOKEN_AFTER_MINUTES = 30;
//    private static final String BASE_URL = ServletUriComponentsBuilder.fromCurrentContextPath().build().toString();

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private ConfirmationTokenService confirmationTokenService;
    private ResetTokenRepository resetTokenRepository;
    private TemplateEngine templateEngine;


    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Page<User> findAllPageable(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getUsersByFiltersSearch(Pageable pageable, String search) {
        return userRepository.getUsersByFiltersSearch(pageable, search);
    }


    public Optional<User> findById(int id) {
        return userRepository.findById(id);
    }

    public UserResponse findByIdUserResponse(int id) {
        UserResponse userResponse = new UserResponse();
        Optional<User> user = userRepository.findById(id);
        Set<String> roles = new HashSet<>();
        if (user.isPresent()) {
            for (Role role : user.get().getRoles()) {
                switch (role.getRole()) {
                    case ROLE_USER:
                        roles.add("user");
                        break;
                    case ROLE_ADMIN:
                        roles.add("admin");
                        break;
                    case ROLE_EMPLOYEE:
                        roles.add("employee");
                        break;
                }
            }
            userResponse.setId(user.get().getId());
            userResponse.setLogin(user.get().getLogin());
            userResponse.setRoles(roles);
            userResponse.setName(user.get().getName());
            userResponse.setLastName(user.get().getLastName());
            userResponse.setSurName(user.get().getSurName());
            userResponse.setEmail(user.get().getEmail());
            userResponse.setPhoneNumber(user.get().getPhoneNumber());
            userResponse.setNameOrganization(user.get().getNameOrganization());
            userResponse.setMainStateRegistrationNumber(user.get().getMainStateRegistrationNumber());
            userResponse.setIdentificationNumber(user.get().getIdentificationNumber());
            userResponse.setPosition(user.get().getPosition());
            userResponse.setSubdivision(user.get().getSubdivision());

            return userResponse;
        } else return new UserResponse();

    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public void deleteById(int id) {
        userRepository.deleteById(id);
    }

    public Set<Role> addRoles(Set<String> strRoles) {
        Set<Role> roleSet = new HashSet<>();
        if (strRoles != null) {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "user":
                        Role userRole = roleRepository.findByRole(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roleSet.add(userRole);
                        break;
                    case "employee":
                        Role employeeRole = roleRepository.findByRole(ERole.ROLE_EMPLOYEE)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roleSet.add(employeeRole);
                        break;
                    case "admin":
                        Role adminRole = roleRepository.findByRole(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roleSet.add(adminRole);
                        break;
                }
            });
        }

        return roleSet;
    }

    public String changePassword(int id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Error: User not found."));
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return "Password update";
        } else return "Passwords do not match";
    }

    public User changeInfoUser(int id, InfoUserRequest infoUserRequest) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Error: User not found."));
        Set<Role> roleSet = addRoles(infoUserRequest.getRole());

        if (!userRepository.existsByLogin(infoUserRequest.getLogin())) {
            user.setLogin(infoUserRequest.getLogin() == null ? user.getLogin() : infoUserRequest.getLogin());
        } else throw new Exception("User login exist");

        user.setName(infoUserRequest.getName() == null ? user.getName() : infoUserRequest.getName());
        user.setLastName(infoUserRequest.getLastName() == null ? user.getLastName() : infoUserRequest.getLastName());
        user.setSurName(infoUserRequest.getSurName() == null ? user.getSurName() : infoUserRequest.getSurName());
        user.setEmail(infoUserRequest.getEmail() == null ? user.getEmail() : infoUserRequest.getEmail());
        user.setPhoneNumber(infoUserRequest.getPhoneNumber() == null ? user.getPhoneNumber() : infoUserRequest.getPhoneNumber());
        user.setRoles(infoUserRequest.getRole() == null ? user.getRoles() : roleSet);

        user.setNameOrganization(infoUserRequest.getNameOrganization() == null ? user.getNameOrganization() :
                infoUserRequest.getNameOrganization());
        user.setMainStateRegistrationNumber(infoUserRequest.getMainStateRegistrationNumber() == null ? user.getMainStateRegistrationNumber() :
                infoUserRequest.getMainStateRegistrationNumber());
        user.setSubdivision(infoUserRequest.getSubdivision() == null ? user.getSubdivision() : infoUserRequest.getSubdivision());
        user.setPosition(infoUserRequest.getPosition() == null ? user.getPosition() : infoUserRequest.getPosition());
        user.setIdentificationNumber(infoUserRequest.getIdentificationNumber() == null ? user.getIdentificationNumber() :
                infoUserRequest.getIdentificationNumber());

        user.setFirstLogin(infoUserRequest.isFirstLogin() || user.isFirstLogin());

        userRepository.save(user);
        return user;
    }

    public String blockUnblockUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Error: User not found."));
        user.setNonBlocked(!user.isNonBlocked());
        userRepository.save(user);
        return user.isNonBlocked() ? "User " + id + " unblock" : "User " + id + " block";
    }


    public String confirmToken(String token) {
        Optional<ConfirmationToken> confirmationToken = confirmationTokenService
                .getToken(token);

        if (confirmationToken.isEmpty())
            return "Invalid token";

        if (confirmationToken.get().getConfirmedAt() != null) {
            return "email already confirmed";
        }

        LocalDateTime expiredAt = confirmationToken.get().getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            return  "token expired";
        }

        confirmationTokenService.setConfirmedAt(token);
        enableAppUser(
                confirmationToken.get().getUser().getEmail());
        return "confirmed";
    }

    public String forgotPassword(String email) {
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByEmail(email));

        if (userOptional.isEmpty()) {
            return "Invalid email id.";
        }

        ResetToken resetToken = new ResetToken();
        resetToken.setUser(userOptional.get());
        resetToken.setToken(generateToken());
        resetToken.setTokenCreationDate(LocalDateTime.now());

        return resetTokenRepository.save(resetToken).getToken();
    }

    public String resetPassword(String token, String password) {
        Optional<ResetToken> resetTokenOptional = Optional
                .ofNullable(resetTokenRepository.findByToken(token));

        if (resetTokenOptional.isEmpty()) {
            return "Invalid token.";
        }

        LocalDateTime tokenCreationDate = resetTokenOptional.get().getTokenCreationDate();

        if (isTokenExpired(tokenCreationDate)) {
            return "Token expired.";
        }

        resetTokenOptional.get().getUser().setPassword(passwordEncoder.encode(password));
        resetTokenOptional.get().setToken(null);
        resetTokenOptional.get().setTokenCreationDate(null);

        resetTokenRepository.save(resetTokenOptional.get());

        return "Your password successfully updated.";
    }

    private String generateToken() {
        return UUID.randomUUID() + UUID.randomUUID().toString();
    }

    private boolean isTokenExpired(final LocalDateTime tokenCreationDate) {

        LocalDateTime now = LocalDateTime.now();
        Duration diff = Duration.between(tokenCreationDate, now);

        return diff.toMinutes() >= EXPIRE_TOKEN_AFTER_MINUTES;
    }

    private int enableAppUser(String email) {
        return userRepository.enableAppUser(email);
    }

    public String buildActivationEmail(String name, String link, String token) {
        Context context = new Context();
        context.setVariable("link", link);
        context.setVariable("name", name);
        context.setVariable("token", token);
        return templateEngine.process("activation-email", context);
    }

    public String buildResetPasswordEmail(String token, String link) {
        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("link", link);
        return templateEngine.process("reset-password-email", context);
    }

}
