package com.example.javadoc0032022.models;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OperationsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String operationName;
    private String ipAddress;

    @CreatedDate
    private LocalDateTime createAt;

    @JsonIncludeProperties(value = {
            "name", "lastName", "surName", "nameOrganization"
    })
    @ManyToOne
    private User user;

    public OperationsHistory(String operationName, LocalDateTime createAt, User user) {
        this.operationName = operationName;
        this.createAt = createAt;
        this.user = user;
    }

    public OperationsHistory(String operationName, String ipAddress, LocalDateTime createAt, User user) {
        this.operationName = operationName;
        this.ipAddress = ipAddress;
        this.createAt = createAt;
        this.user = user;
    }
}
