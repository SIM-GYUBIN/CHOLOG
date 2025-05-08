package com.ssafy.cholog.domain.user.service;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import com.ssafy.cholog.domain.user.dto.request.SignupRequest;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public LoginResult login(LoginRequest loginRequest) {
        return null;
    }

    @Transactional
    public Void signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, signupRequest.getEmail());
        }

        if (userRepository.existsByNickname(signupRequest.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS, signupRequest.getNickname());
        }

        User user = signupRequest.toEntity(passwordEncoder);
        userRepository.save(user);

        return null;
    }
}
