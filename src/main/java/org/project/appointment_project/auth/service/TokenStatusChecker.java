package org.project.appointment_project.auth.service;

public interface TokenStatusChecker {
    boolean  isTokenInvalidated(String tokenHash);
    boolean  isTokenBlacklisted(String tokenHash);
}
