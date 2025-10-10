package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckEmailResponseDTO {
    private String provider;
    private String status;
    private String redirect;
}