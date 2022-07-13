package com.example.javadoc0032022.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentFilterResponse {
    private String code;
    private int value;
}
