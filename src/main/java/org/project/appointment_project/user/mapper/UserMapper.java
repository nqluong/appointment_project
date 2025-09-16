package org.project.appointment_project.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.project.appointment_project.user.dto.response.UserResponse;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "firstName", source = "userProfile.firstName")
    @Mapping(target = "lastName", source = "userProfile.lastName")
    @Mapping(target = "roles", source = "userRoles", qualifiedByName = "mapRoles")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isEmailVerified", source = "emailVerified")
    UserResponse toResponse(User user);

    @Named("mapRoles")
    default List<String> mapRoles(List<UserRole> userRoles) {
        if (userRoles == null) {
            return List.of();
        }
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }
}