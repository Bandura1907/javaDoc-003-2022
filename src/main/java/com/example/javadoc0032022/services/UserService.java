package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.token.ConfirmationToken;
import com.example.javadoc0032022.payload.request.InfoUserRequest;
import com.example.javadoc0032022.payload.response.UserResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private ConfirmationTokenService confirmationTokenService;

    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public UserResponse findById(int id) {
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
            return new UserResponse(user.get().getId(), user.get().getLogin(), user.get().getPassword(), roles, user.get().getName(),
                    user.get().getLastName(), user.get().getSurName(), user.get().getEmail(), user.get().getPhoneNumber(),
                    user.get().getCountDocuments(), user.get().isExistEcp(), user.get().isTimeLocked(), user.get().isPasswordExpired(),
                    user.get().isNonBlocked(), user.get().getLoginAttempts(), user.get().getBlockTime());
        } else return new UserResponse();

    }

    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
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
        Set<Role> roleSet = addRoles(infoUserRequest.getRoles());

        if (!userRepository.existsByLogin(infoUserRequest.getLogin())) {
            user.setLogin(infoUserRequest.getLogin() == null ? user.getLogin() : infoUserRequest.getLogin());
        } else throw new Exception("User login exist");

        user.setName(infoUserRequest.getName() == null ? user.getName() : infoUserRequest.getName());
        user.setLastName(infoUserRequest.getLastName() == null ? user.getLastName() : infoUserRequest.getLastName());
        user.setSurName(infoUserRequest.getSurName() == null ? user.getSurName() : infoUserRequest.getSurName());
        user.setEmail(infoUserRequest.getEmail() == null ? user.getEmail() : infoUserRequest.getEmail());
        user.setPhoneNumber(infoUserRequest.getPhoneNumber() == null ? user.getPhoneNumber() : infoUserRequest.getPhoneNumber());
        user.setRoles(infoUserRequest.getRoles() == null ? user.getRoles() : roleSet);
        user.setCountDocuments(infoUserRequest.getCountDocuments() == null ? user.getCountDocuments() : infoUserRequest.getCountDocuments());
//        user.setExistEcp(infoUserRequest.isExistEcp() || user.isExistEcp());
//        user.setPasswordExpired(infoUserRequest.isPasswordExpired() || user.isPasswordExpired());

        userRepository.save(user);
        return user;
    }

    public String blockUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Error: User not found."));
        if (user.isNonBlocked()) {
            user.setNonBlocked(false);
            userRepository.save(user);
            return "User " + id + " blocked";
        } else {
            user.setNonBlocked(true);
            userRepository.save(user);
            return "User " + id + " unlock";
        }
    }

    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() ->
                        new IllegalStateException("token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException("email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("token expired");
        }

        confirmationTokenService.setConfirmedAt(token);
        enableAppUser(
                confirmationToken.getUser().getEmail());
        return "confirmed";
    }

    private int enableAppUser(String email) {
        return userRepository.enableAppUser(email);
    }

    public String buildEmail(String name, String link) {
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
