package org.project.appointment_project.common.security.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.common.util.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAspect {
    private final SecurityUtils securityUtils;

    @Before("@annotation(requireOwnershipOrAdmin)")
    public void validateOwnershipOrAdmin(JoinPoint joinPoint, RequireOwnershipOrAdmin requireOwnershipOrAdmin) {
        Object[] args = joinPoint.getArgs();
        String userIdParamName = requireOwnershipOrAdmin.userIdParam();
        String[] allowedRoles = requireOwnershipOrAdmin.allowedRoles();

        if(args.length == 0 || !(args[0] instanceof UUID)){
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        UUID targetUserId = (UUID) args[0];
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Check if user is admin
        if (securityUtils.isCurrentUserAdmin()) {
            log.info("Admin access granted for user: {} by admin: {}", targetUserId, currentUserId);
            return;
        }

        // Check if user is accessing their own profile
        if (!currentUserId.equals(targetUserId)) {
            log.warn("User {} attempted to access profile of user {}", currentUserId, targetUserId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // Check if user has required role
        boolean hasAllowedRole = Arrays.stream(allowedRoles)
                .anyMatch(securityUtils::hasRole);

        if (!hasAllowedRole) {
            log.warn("User {} does not have required role for this operation", currentUserId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        log.info("Access granted for user: {} to modify their own profile", currentUserId);
    }
}
