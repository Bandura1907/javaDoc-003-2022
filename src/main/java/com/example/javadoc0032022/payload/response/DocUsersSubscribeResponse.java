package com.example.javadoc0032022.payload.response;

import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocUsersSubscribeResponse {
    private int idDocument;
    private DocumentStatus documentStatus;
    private User userEmployee;
    private User userClient;
}
