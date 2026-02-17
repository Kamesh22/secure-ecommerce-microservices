package com.ecommerce.auth.mapper;

import com.ecommerce.auth.dto.AuthResponseDTO;
import com.ecommerce.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    AuthResponseDTO userToAuthResponseDTO(User user);

    User authResponseDTOToUser(AuthResponseDTO authResponseDTO);
}
