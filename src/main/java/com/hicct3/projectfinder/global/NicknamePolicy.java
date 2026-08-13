package com.hicct3.projectfinder.global;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class NicknamePolicy {
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;
    private static final String ALLOWED_PATTERN = "^[가-힣a-zA-Z0-9.]+$";
    private static final List<String> RESERVED_TERMS = List.of(
            "admin",
            "administrator",
            "official",
            "support",
            "zzoin",
            "관리자",
            "운영자",
            "고객센터",
            "고객지원",
            "쪼인",
            "deleted",
            "탈퇴",
            "삭제",
            "비활성화"
    );

    private NicknamePolicy() {
    }

    public static String normalizeAndValidate(String nickname) {
        if (nickname == null) {
            throw new GeneralException(ErrorCode.RESERVED_NICKNAME);
        }

        String normalized = Normalizer.normalize(nickname.trim(), Normalizer.Form.NFKC);
        if (normalized.length() < MIN_LENGTH
                || normalized.length() > MAX_LENGTH
                || !normalized.matches(ALLOWED_PATTERN)) {
            throw new GeneralException(ErrorCode.INVALID_NICKNAME);
        }
        String comparable = normalized.toLowerCase(Locale.ROOT);
        if (RESERVED_TERMS.stream().anyMatch(comparable::contains)) {
            throw new GeneralException(ErrorCode.RESERVED_NICKNAME);
        }
        return normalized;
    }
}
