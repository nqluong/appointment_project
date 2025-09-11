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


    //User errors
    USERNAME_ALREADY_EXISTS(2001, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(2003, "User not found", HttpStatus.NOT_FOUND),
    REGISTRATION_FAILED(2004, "User registration failed", HttpStatus.BAD_REQUEST),
    LICENSE_NUMBER_ALREADY_EXISTS(2005, "License number already exists", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND(2006, "Role not found", HttpStatus.NOT_FOUND),
    EMAIL_NOT_VERIFIED(2007, "Email not verified", HttpStatus.UNAUTHORIZED),

    SPECIALTY_NOT_FOUND(3001, "Specialty not found", HttpStatus.NOT_FOUND);


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
