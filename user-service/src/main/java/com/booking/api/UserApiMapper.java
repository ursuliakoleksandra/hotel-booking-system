package com.booking.api;
import com.booking.api.dto.UserDTO;
import com.booking.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserApiMapper {
    UserDTO toDto(User user);
    User toDomain(UserDTO dto);
}