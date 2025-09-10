package org.project.appointment_project.common.security.jwt.filter;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtUserPrincipal {
    UUID  userId;
    String username;
    List<String> roles;

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(List<String> roles) {
        if(this.roles == null) return false;
        for(String role : roles) {
            if(this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
