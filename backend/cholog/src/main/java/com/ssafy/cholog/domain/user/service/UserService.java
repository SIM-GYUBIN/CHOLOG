package com.ssafy.cholog.domain.user.service;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import com.ssafy.cholog.domain.user.dto.request.SignupRequest;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.common.constants.UserType;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResult login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email", loginRequest.getEmail()));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getId()), UserType.USER);
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(user.getId()), UserType.USER);

        return LoginResult.of(accessToken, refreshToken);
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
