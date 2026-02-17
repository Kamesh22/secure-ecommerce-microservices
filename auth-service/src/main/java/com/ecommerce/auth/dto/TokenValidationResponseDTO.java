package com.ecommerce.auth.dto;

import com.ecommerce.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponseDTO {

    private Long userId;
    private String username;
    private UserRole role;
    private Boolean valid;
    private String message;

}
