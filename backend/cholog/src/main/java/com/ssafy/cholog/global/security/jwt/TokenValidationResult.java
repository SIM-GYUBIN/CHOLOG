package com.ssafy.cholog.global.security.jwt;

public record TokenValidationResult(boolean isValid, TokenError error) {
}
