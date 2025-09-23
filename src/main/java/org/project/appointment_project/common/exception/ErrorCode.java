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
    DUPLICATE_SLOT_IDS(7010, "Duplicate slot IDs found", HttpStatus.NOT_FOUND),


    SLOT_ALREADY_BOOKED(8001, "The selected slot has already been booked by another patient", HttpStatus.CONFLICT),
    INVALID_SLOT_DOCTOR(8002, "The selected slot does not belong to the chosen doctor", HttpStatus.BAD_REQUEST),
    PATIENT_NOT_FOUND(8003, "Patient does not exist", HttpStatus.NOT_FOUND),
    PATIENT_INACTIVE(8004, "Patient account is inactive", HttpStatus.BAD_REQUEST),
    PATIENT_NO_ROLE(8005, "User does not have patient role", HttpStatus.UNAUTHORIZED),
    DOCTOR_NOT_FOUND(8006, "Doctor does not exist", HttpStatus.NOT_FOUND),
    DOCTOR_INACTIVE(8007, "Doctor account is inactive", HttpStatus.BAD_REQUEST),
    DOCTOR_NOT_APPROVED(8008, "Doctor has not been approved", HttpStatus.BAD_REQUEST),
    PATIENT_OVERLAPPING_APPOINTMENT(8009, "Patient already has an overlapping appointment at this time", HttpStatus.CONFLICT),
    PATIENT_TOO_MANY_PENDING(8010, "Patient has too many pending appointments (maximum 3)", HttpStatus.BAD_REQUEST),
    APPOINTMENT_CREATION_FAILED(8011, "Failed to create appointment", HttpStatus.BAD_REQUEST),
    CONCURRENT_BOOKING_CONFLICT(8012, "Conflict occurred during concurrent booking", HttpStatus.CONFLICT),
    APPOINTMENT_FETCH_FAILED(8013, "Failed to fetch appointments", HttpStatus.BAD_REQUEST),
    APPOINTMENT_NOT_PAYABLE(8014, "Appointment is not in a payable status", HttpStatus.BAD_REQUEST),
    APPOINTMENT_NOT_FOUND(8015,"Appointment not found", HttpStatus.NOT_FOUND),

    PAYMENT_NOT_FOUND(9001, "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_INVALID_AMOUNT(9002, "Payment amount must be greater than 0", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_STATUS(9003, "Invalid payment status transition", HttpStatus.BAD_REQUEST),
    PAYMENT_PROCESSING_FAILED(9004, "Payment processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_GATEWAY_ERROR(9005, "Payment gateway error", HttpStatus.BAD_GATEWAY),
    PAYMENT_INVALID_SIGNATURE(9006, "Invalid payment signature", HttpStatus.BAD_REQUEST),
    PAYMENT_EXPIRED(9007, "Payment has expired", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_PROCESSED(9008, "Payment has already been processed", HttpStatus.CONFLICT),
    PAYMENT_CANCELLED(9009, "Payment has been cancelled", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_EXISTS(9010, "Payment already exists for this appointment", HttpStatus.CONFLICT),
    INVALID_PAYMENT_TYPE(9011, "Payment type invalid ", HttpStatus.BAD_REQUEST),
    PAYMENT_QUERY_FAILED(9012, "Payment query failed", HttpStatus.BAD_REQUEST),
    REFUND_PROCESSING_ERROR(9013, "Error processing refund", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_GATEWAY_ERROR(9014, "Refund gateway error", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_TRANSACTION_FAILED(9015, "Refund transaction failed", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_NOT_FOUND(9016, "Refund not found", HttpStatus.NOT_FOUND),
    REFUND_ALREADY_PROCESSED(9017, "Refund already processed", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_REFUNDABLE(9018, "Payment is not refundable", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_REFUNDED(9019, "Payment has been fully refunded", HttpStatus.BAD_REQUEST),
    REFUND_PERIOD_EXPIRED(9020, "Refund period has expired", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_REFUND_AMOUNT(9021, "Refund amount exceeds available amount", HttpStatus.BAD_REQUEST),
    INVALID_REFUND_AMOUNT(9022, "Invalid refund amount", HttpStatus.BAD_REQUEST),
    REFUND_AMOUNT_TOO_SMALL(9023, "Refund amount is too small", HttpStatus.BAD_REQUEST),
    REFUND_AMOUNT_TOO_LARGE(9024, "Refund amount is too large", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_FULLY_REFUNDED(9025, "Payment has been fully refunded", HttpStatus.BAD_REQUEST),
    APPOINTMENT_DATE_PASSED(9026, "Cannot refund payment for past appointments", HttpStatus.BAD_REQUEST),



    VNPAY_INVALID_RESPONSE(1101, "Invalid VNPay response", HttpStatus.BAD_REQUEST),
    VNPAY_SIGNATURE_VERIFICATION_FAILED(1102, "VNPay signature verification failed", HttpStatus.BAD_REQUEST),
    VNPAY_TRANSACTION_FAILED(1103, "VNPay transaction failed", HttpStatus.BAD_REQUEST)
    ;



    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
