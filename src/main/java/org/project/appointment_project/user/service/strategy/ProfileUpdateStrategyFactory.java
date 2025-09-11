package org.project.appointment_project.user.service.strategy;

import lombok.RequiredArgsConstructor;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileUpdateStrategyFactory {
    private final List<ProfileUpdateStrategy> strategies;

    /**
     * Lấy strategy phù hợp dựa trên set of roles
     */
    public ProfileUpdateStrategy getStrategy(Set<String> roles) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(roles))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ROLE_OPERATION,
                        "No suitable update strategy found for roles: " + roles));
    }
}
