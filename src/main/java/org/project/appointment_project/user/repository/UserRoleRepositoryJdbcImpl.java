package org.project.appointment_project.user.repository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.user.dto.response.RoleInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleRepositoryJdbcImpl implements UserRoleRepositoryJdbc {
    JdbcTemplate jdbcTemplate;

    //Lấy danh sách các quyền của user
    @Override
    public List<String> getUserRoleNames(UUID userId) {
        String sql = """
                SELECT r.name 
                FROM user_roles ur 
                JOIN roles r ON ur.role_id = r.id 
                WHERE ur.user_id = ? 
                  AND ur.is_active = true 
                  AND r.is_active = true
                  AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
                """;

        return jdbcTemplate.queryForList(sql, String.class, userId);
    }


    //Cấp quyền cho user
    @Override
    public void assignRoleToUser(UUID userId, UUID roleId, UUID assignedBy, LocalDateTime expiresAt) {
        String sql = """
                INSERT INTO user_roles (id, user_id, role_id, assigned_by, assigned_at, is_active, expires_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, true, ?)
                """;

        jdbcTemplate.update(sql, UUID.randomUUID(), userId, roleId, assignedBy, expiresAt);
    }

    // Cấp quyền khi đăng ký
    @Override
    public void assignRoleToUserOnRegistration(UUID userId, UUID roleId) {
        String sql = """
            INSERT INTO user_roles (id, user_id, role_id, assigned_at, is_active)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, true)
            """;

        jdbcTemplate.update(sql, UUID.randomUUID(), userId, roleId);
    }

    //Kiểm tra có quyền này không
    @Override
    public boolean hasActiveRole(UUID userId, UUID roleId) {
        String sql = """
                SELECT COUNT(*)
                FROM user_roles ur
                JOIN roles r ON ur.role_id = r.id
                WHERE ur.user_id = ?
                    AND ur.role_id = ?
                    AND ur.is_active = true
                    AND r.is_active = true
                    AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, roleId);
        return count != null && count > 0;
    }


     //Vô hiệu hóa quyền của user
     @Override
    public void deactivateUserRole(UUID userId, UUID roleId) {
        String sql = """
            UPDATE user_roles 
            SET is_active = false 
            WHERE user_id = ? AND role_id = ? AND is_active = true
            """;
        jdbcTemplate.update(sql, userId, roleId);
    }

     //Vô hiệu hóa tất cả quyền của user
     @Override
    public void deactivateAllUserRoles(UUID userId) {
        String sql = """
            UPDATE user_roles 
            SET is_active = false 
            WHERE user_id = ? AND is_active = true
            """;

        jdbcTemplate.update(sql, userId);
    }


    @Override
    public List<RoleInfo> getAvailableRoles() {
        String sql = """
            SELECT id, name, description 
            FROM roles 
            WHERE is_active = true 
            ORDER BY name
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> RoleInfo.builder()
                .id(UUID.fromString(rs.getString("id")))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .build());
    }

    /**
     * Cập nhật thời hạn quyền
     */
    @Override
    public void updateRoleExpiration(UUID userId, UUID roleId, LocalDateTime newExpiresAt) {
        String sql = """
            UPDATE user_roles 
            SET expires_at = ? 
            WHERE user_id = ? AND role_id = ? AND is_active = true
            """;

        jdbcTemplate.update(sql, newExpiresAt, userId, roleId);
    }
}
