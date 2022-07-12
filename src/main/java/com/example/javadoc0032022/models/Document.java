package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private LocalDateTime createAt;

    @Lob
    private byte[] file;

    @JsonProperty("package")
    @ManyToOne
    @JsonBackReference
    private Package aPackage;

////    @JsonIgnore
//    @ManyToOne
//    private User senderUser;
//
////    @JsonIgnore
//    @ManyToOne
//    private User receiverUser;
}
