package org.project.appointment_project.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.filter.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtils {

    //Lay thong tin user hien tai tu Security Context
    public static JwtUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return (JwtUserPrincipal) principal;
    }

    //Lay id cua user hien tai
    public static UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    //Lay username cua user hien tai
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

   //Kiem tra user hien tai co role hay khong
    public static boolean hasRole(String role) {
        try {
            return getCurrentUser().hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }

    //Kiem tra user hien tai co bat ky role nao trong danh sach role truyen vao hay khong
    public static boolean hasAnyRole(List<String> roles) {
        try {
            return getCurrentUser().hasAnyRole(roles);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    public static boolean isPatient() {
        return hasRole("PATIENT");
    }
}
