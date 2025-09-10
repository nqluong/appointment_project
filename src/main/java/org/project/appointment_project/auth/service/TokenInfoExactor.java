package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.response.TokenInfo;

// Lay thong tin tu token
public interface TokenInfoExactor {
    TokenInfo extractTokenInfo(String token);
}
