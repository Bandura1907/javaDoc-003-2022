package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.token.ConfirmationToken;
import com.example.javadoc0032022.models.token.RefreshToken;
import com.example.javadoc0032022.models.token.ResetToken;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(value = {
        "isTimeLocked", "isPasswordExpired", "loginAttempts", "blockTime", "isNonBlocked", "confirmationTokenList",
        "refreshToken", "packageSenderList", "packageReceiverList", "resetToken", "isTimeLocked", "packages", "operationsHistories"
})
public class User implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String login;

    @JsonIgnore
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    private String name;
    private String lastName;
    private String surName;
    private String email;
    private String phoneNumber;

    private String nameOrganization;
    private String mainStateRegistrationNumber;
    private String identificationNumber;
    private String position;
    private String subdivision;


    private boolean isTimeLocked;
    private boolean isPasswordExpired;
    private int loginAttempts;
    private long blockTime;
    private boolean isNonBlocked;

    private boolean enabled;
    private boolean firstLogin;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ConfirmationToken> confirmationTokenList;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private RefreshToken refreshToken;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private ResetToken resetToken;

    @OneToMany(mappedBy = "senderUser", cascade = CascadeType.ALL)
    private List<Package> packageSenderList;

    @OneToMany(mappedBy = "receiverUser", cascade = CascadeType.ALL)
    private List<Package> packageReceiverList;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Package> packages;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<OperationsHistory> operationsHistories;


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getRole().name()))
                .collect(Collectors.toList());
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return login;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return isNonBlocked;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
