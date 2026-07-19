package com.hicct3.projectfinder.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum SortType {

    LATEST(Sort.by(Sort.Direction.DESC, "createdAt"));

    private final Sort sort;

    public static SortType from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }

        try {
            return SortType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LATEST;
        }
    }
}