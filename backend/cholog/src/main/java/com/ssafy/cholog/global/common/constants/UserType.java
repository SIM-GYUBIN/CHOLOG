package com.ssafy.cholog.global.common.constants;

public enum UserType {
    USER, ADMIN;

    public String getLowerCaseName() {
        return this.name().toLowerCase();
    }
}
