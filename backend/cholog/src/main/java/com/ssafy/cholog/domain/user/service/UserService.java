package com.ssafy.cholog.domain.user.service;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    public LoginResult login(LoginRequest loginRequest) {
        return null;
    }
}
