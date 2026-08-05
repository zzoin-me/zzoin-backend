package com.hicct3.projectfinder.entity.enums;

public enum PostBoardType {
    ALL,
    MINE,
    COMMENTS,
    LIKES,
    SAVED;

    public static PostBoardType from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return PostBoardType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
