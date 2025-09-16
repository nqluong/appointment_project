package org.project.appointment_project.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1000, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1002, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1003, "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1004, "Resource not found", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(1005, "Validation error", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1006, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1007, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1008, "Invalid token", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(1009, "Account is disabled", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED(1010, "Account is not verified", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1011, "Access denied", HttpStatus.FORBIDDEN),
    TOKEN_GENERATION_FAILED(1012, "Token generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_PARSE_ERROR(1013, "Error parsing token", HttpStatus.BAD_REQUEST),
    INVALID_ROLE(1014,"Invalid user role", HttpStatus.BAD_REQUEST),

    MISSING_REQUIRED_FIELD(1015, "Required field is missing", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(1016, "Invalid input provided", HttpStatus.BAD_REQUEST),
    INVALID_ROLE_OPERATION(1017, "Operation not allowed for this role",HttpStatus.BAD_REQUEST),
    INVALID_UUID_FORMAT(1018, "Invalid uuid format", HttpStatus.BAD_REQUEST),
    ROLE_ALREADY_ASSIGNED(1019, "Role already assigned to user", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND(1020, "Role not found for user", HttpStatus.NOT_FOUND),
    INVALID_EXPIRATION_DATE(1023, "Expiration date must be in the future", HttpStatus.BAD_REQUEST),
    ROLE_ASSIGNMENT_FAILED(1022, "Failed to assign role to user", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_REVOCATION_FAILED(1023, "Failed to revoke role from user", HttpStatus.INTERNAL_SERVER_ERROR),
    ROLE_UPDATE_FAILED(1024, "Failed to update role", HttpStatus.INTERNAL_SERVER_ERROR),
    INSUFFICIENT_PRIVILEGES(1025, "Insufficient privileges to perform this action", HttpStatus.FORBIDDEN),

    //User errors
    USERNAME_ALREADY_EXISTS(2001, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(2003, "User not found", HttpStatus.NOT_FOUND),
    REGISTRATION_FAILED(2004, "User registration failed", HttpStatus.BAD_REQUEST),
    LICENSE_NUMBER_ALREADY_EXISTS(2005, "License number already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_VERIFIED(2007, "Email not verified", HttpStatus.UNAUTHORIZED),
    USER_CANNOT_BE_DELETED(2008, "User cannot be deleted", HttpStatus.BAD_REQUEST),
    USER_NOT_DELETED(2009, "User is not in deleted state", HttpStatus.BAD_REQUEST),
    USER_ALREADY_DELETED(2010, "User is already deleted", HttpStatus.BAD_REQUEST),

    SPECIALTY_NOT_FOUND(3001, "Specialty not found", HttpStatus.NOT_FOUND),

    INVALID_FILE_FORMAT(4001, "Invalid file format. Only JPG, PNG, GIF files are allowed", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED(4002, "File size exceeded. Maximum allowed size is 5MB", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(4003, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(4004, "Failed to delete old file", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_CONTENT_TYPE(4005, "Invalid file content type", HttpStatus.BAD_REQUEST),
    FILE_NOT_PROVIDED(4006, "File not provided or is empty", HttpStatus.BAD_REQUEST),
    PHOTO_UPLOAD_ERROR(4007, "Error occurred while uploading photo", HttpStatus.INTERNAL_SERVER_ERROR),
    DIRECTORY_CREATION_FAILED(4008, "Failed to create upload directory", HttpStatus.INTERNAL_SERVER_ERROR),

    SPECIALTY_NAME_EXISTS(5001, "Specialty name already exists", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_PERMISSION(5002,"User does not have sufficient permissions" ,HttpStatus.BAD_REQUEST ),
    SCHEDULE_ALREADY_EXISTS(5003, "Doctor already has a schedule configured", HttpStatus.CONFLICT),
    SCHEDULE_NOT_FOUND(5004, "Doctor schedule not found", HttpStatus.NOT_FOUND),
    DUPLICATE_SCHEDULE_DAY(5004, "Duplicate day of week in schedule entries", HttpStatus.CONFLICT),
    INVALID_TIME_RANGE(5005, "Invalid time range: start time must be before end time", HttpStatus.BAD_REQUEST),
    INVALID_WORKING_HOURS(5006, "Working hours must be between 6:00 AM and 11:00 PM", HttpStatus.BAD_REQUEST),
    INVALID_SLOT_DURATION(5007, "Slot duration exceeds total working time", HttpStatus.CONFLICT),

    INVALID_SEARCH_CRITERIA(6001, "Invalid search criteria provided", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(6002, "Invalid date range for search", HttpStatus.BAD_REQUEST),
    INVALID_EXPERIENCE_RANGE(6003, "Invalid experience range", HttpStatus.BAD_REQUEST),
    INVALID_FEE_RANGE(6004, "Invalid consultation fee range", HttpStatus.BAD_REQUEST),
    DATABASE_ERROR(6005, "Database error occurred during search", HttpStatus.BAD_REQUEST),

    SLOT_NOT_FOUND(7001, "Slot not found", HttpStatus.NOT_FOUND),
    SLOT_NOT_AVAILABLE(7002, "Slot is not available", HttpStatus.NOT_FOUND),
    SLOT_ACCESS_DENIED(7003, "Access denied to slot", HttpStatus.UNAUTHORIZED),
    SLOT_ALREADY_RESERVED(7004, "Slot is already reserved", HttpStatus.CONFLICT),
    SLOT_ALREADY_AVAILABLE(7005, "Slot is already available", HttpStatus.CONFLICT),
    SLOT_IN_PAST(7006, "Cannot modify slot in the past", HttpStatus.BAD_REQUEST),
    SLOT_UPDATE_FAILED(7007, "Failed to update slot status", HttpStatus.BAD_REQUEST),
    INVALID_SLOT_OPERATION(7008, "Invalid slot operation", HttpStatus.BAD_REQUEST),
    BULK_OPERATION_LIMIT_EXCEEDED(7009, "Bulk operation limit exceeded", HttpStatus.BAD_REQUEST),
    DUPLICATE_SLOT_IDS(7010, "Duplicate slot IDs found", HttpStatus.NOT_FOUND);
    ;


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
