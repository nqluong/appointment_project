package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.PhotoUploadRequest;
import org.project.appointment_project.user.dto.response.PhotoUploadResponse;

import java.util.UUID;

public interface PhotoUploadService {
    /**
     * Upload và cập nhật ảnh đại diện cho người dùng
     * @param userId ID của người dùng
     * @param request Yêu cầu upload ảnh
     * @return Đường dẫn ảnh mới
     */
    PhotoUploadResponse uploadUserPhoto(UUID userId, PhotoUploadRequest request);
}
