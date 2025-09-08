package org.project.appointment_project.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1000, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1001, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1002, "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1003, "Resource not found", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(1004, "Validation error", HttpStatus.BAD_REQUEST),

    //User errors
    USERNAME_ALREADY_EXISTS(2001, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(2003, "User not found", HttpStatus.NOT_FOUND),
    REGISTRATION_FAILED(2004, "User registration failed", HttpStatus.BAD_REQUEST),
    LICENSE_NUMBER_ALREADY_EXISTS(2005, "License number already exists", HttpStatus.CONFLICT),


    SPECIALTY_NOT_FOUND(3001, "Specialty not found", HttpStatus.NOT_FOUND)
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
