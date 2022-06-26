package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.token.ConfirmationToken;
import com.example.javadoc0032022.models.token.RefreshToken;
import com.example.javadoc0032022.models.token.ResetToken;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String login;

    @JsonIgnore
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    private String name;
    private String lastName;
    private String surName;

    private String email;
    private String phoneNumber;
    private int countDocuments;

    private boolean existEcp;

    private boolean isTimeLocked;
    private boolean isPasswordExpired;
    private int loginAttempts;
    private long blockTime;
    private boolean isNonBlocked;
    private boolean enabled;
    private boolean firstLogin;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ConfirmationToken> confirmationTokenList;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private RefreshToken refreshToken;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private ResetToken resetToken;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Document> documentList;

}
