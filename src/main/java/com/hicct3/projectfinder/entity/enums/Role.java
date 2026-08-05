package com.hicct3.projectfinder.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    OWNER("관리자"),
    MEMBER("회원");

    private final String name;

    public String getName() {
        return this.name;
    }
}
