package com.example.javadoc0032022.models;

import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String docName;

    private boolean draft;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

//    @JsonIgnore
    @Lob
    private byte[] file;

//    @JsonIgnore
    @ManyToOne
    private User senderUser;

//    @JsonIgnore
    @ManyToOne
    private User receiverUser;
}
