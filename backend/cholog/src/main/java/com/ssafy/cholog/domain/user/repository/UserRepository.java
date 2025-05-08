package com.ssafy.cholog.domain.user.repository;

import com.ssafy.cholog.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@NotBlank(message = "이메일을 입력해주세요.") String email);

    boolean existsByNickname(@NotBlank(message = "닉네임을 입력해주세요.") String nickname);
}
