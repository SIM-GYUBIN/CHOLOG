package com.ssafy.cholog.domain.user.service;

import com.ssafy.cholog.domain.user.dto.LoginResult;
import com.ssafy.cholog.domain.user.oauth.dto.OAuthUserInfo;
import com.ssafy.cholog.domain.user.dto.request.LoginRequest;
import com.ssafy.cholog.domain.user.dto.request.SignupRequest;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.enums.Provider;
import com.ssafy.cholog.domain.user.oauth.strategy.OAuthStrategy;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.common.constants.UserType;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final Map<Provider, OAuthStrategy> oAuthStrategyMap;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResult login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "email", loginRequest.getEmail()));

        // userType이 일반회원인지 확인
        if (user.getUserType() != UserType.GENERAL) {
            throw new CustomException(ErrorCode.NOT_GENERAL_USER)
                    .addParameter("email", loginRequest.getEmail());
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(user.getId()), UserType.GENERAL);
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(user.getId()), UserType.GENERAL);

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

    @Transactional
    public LoginResult handleOAuthLogin(Provider provider, String code) {

        // 1. 전략 가져오기
        OAuthStrategy strategy = oAuthStrategyMap.get(provider);
        if (strategy == null) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_PROVIDER, "provider", provider.name());
        }

        // 2. 소셜 로그인으로 유저 정보 받아오기
        OAuthUserInfo userInfoFromOAuth = strategy.getUserInfo(code);

        // 3. 기존 회원인지 확인
        Optional<User> existingUser = userRepository.findByEmail(userInfoFromOAuth.getEmail());

        // UserType이 일반회원이면 exeption 발생. 그 후 null이면 회원가입 진행
        if (existingUser.isPresent() && existingUser.get().getUserType() == UserType.GENERAL) {
            throw new CustomException(ErrorCode.NOT_OAUTH_USER)
                    .addParameter("email", userInfoFromOAuth.getEmail());
        }

        // 4. 신규 회원이면 회원가입 진행
        User user = existingUser.orElseGet(() -> createOauthUser(userInfoFromOAuth, provider));
        String userId = user.getId().toString();
        UserType userType = user.getUserType();

        String accessToken = jwtTokenProvider.createAccessToken(userId, userType);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, userType);

        // Redis에 RefreshToken 저장
//        authRedisService.saveRefreshToken(userId, refreshToken);

        return LoginResult.of(accessToken, refreshToken);
    }

    private User createOauthUser(OAuthUserInfo userInfo, Provider provider) {
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .password(UUID.randomUUID().toString())
                .nickname(userInfo.getNickname())
                .userType(UserType.getUserTypeByProvider(provider))
                .build();

        return userRepository.save(newUser);
    }
}
