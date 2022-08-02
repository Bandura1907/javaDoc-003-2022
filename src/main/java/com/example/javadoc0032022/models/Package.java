package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.models.enums.PackageType;
import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String comment;
    private String name;
    private boolean draft;
    private LocalDateTime createAt;

    @Enumerated(EnumType.STRING)
    private DocumentStatus packageStatus;

    @Enumerated(EnumType.STRING)
    private PackageType packageType;

    @OneToMany(mappedBy = "aPackage", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Document> documents;

    @ManyToOne
    @JsonIgnore
    private User user;

    //    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    @JsonIncludeProperties(value = {
            "name", "lastName", "surName", "nameOrganization"
    })
    private User senderUser;

    //    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    @JsonIncludeProperties(value = {
            "name", "lastName", "surName", "nameOrganization"
    })
    private User receiverUser;


}
