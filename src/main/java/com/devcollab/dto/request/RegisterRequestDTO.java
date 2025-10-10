package com.devcollab.dto.request;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
}
