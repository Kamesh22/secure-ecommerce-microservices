package com.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ecommerce.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    private Long userId;
    private String username;
    private String email;
    private UserRole role;
    private String token;
    private String message;
}
