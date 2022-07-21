package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.enums.DocumentStatus;
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
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
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

    @OneToMany(mappedBy = "aPackage", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Document> documents;

//    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    private User senderUser;

//    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    private User receiverUser;

//    @ManyToOne
//    private User senderUser;
//
//    @ManyToOne
//    private User receiverUser;
}
