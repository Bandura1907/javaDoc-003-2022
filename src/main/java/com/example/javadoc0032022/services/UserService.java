package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.payload.request.InfoUserRequest;
import com.example.javadoc0032022.payload.response.UserResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}
