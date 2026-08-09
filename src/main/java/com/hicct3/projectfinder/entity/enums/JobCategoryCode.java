package com.hicct3.projectfinder.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobCategoryCode {
    PLANNING("기획"),
    DESIGN("디자인"),
    DEVELOPMENT("개발"),
    MARKETING("마케팅");

    private final String name;

    public String getName() {
        return this.name;
    }
}
