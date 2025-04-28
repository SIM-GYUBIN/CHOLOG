package com.ssafy.cholog.domain.auth.service;

import com.ssafy.cholog.domain.auth.dto.LoginResult;
import com.ssafy.cholog.domain.auth.dto.request.LoginRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    public LoginResult login(LoginRequest loginRequest) {
        return null;
    }
}
