package com.hicct3.projectfinder.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobCategoryCode {
    PLANNING("관리자"),
    DESIGN("회원"),
    DEVELOPMENT("개발"),
    MARKETING("마케팅"),
    ETC("기타");

    private final String name;

    public String getName() {
        return this.name;
    }
}
