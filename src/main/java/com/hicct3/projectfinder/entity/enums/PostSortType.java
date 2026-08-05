package com.hicct3.projectfinder.entity.enums;

public enum PostSortType {
    LATEST,
    POPULAR;

    public static PostSortType from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        try {
            return PostSortType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LATEST;
        }
    }
}
