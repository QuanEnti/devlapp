package com.devcollab.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailOnlyDTO {
    @Email
    @NotBlank
    private String email;
}