package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String docName;

    private boolean draft;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Lob
    private byte[] file;

    @JsonProperty("package")
    @ManyToOne
    private Package aPackage;

////    @JsonIgnore
//    @ManyToOne
//    private User senderUser;
//
////    @JsonIgnore
//    @ManyToOne
//    private User receiverUser;
}