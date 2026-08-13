package com.hicct3.projectfinder.global;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NicknamePolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "myADMINaccount",
            "administrator01",
            "ZzoinOfficial",
            "customerSupport",
            "zzoinUser",
            "쪼인관리자",
            "프로젝트운영자",
            "고객센터직원",
            "고객지원팀",
            "쪼인회원",
            "MyDeLeTeDAccount",
            "탈퇴회원",
            "삭제된사용자",
            "비활성화계정"
    })
    void rejectsEveryReservedTermAsSubstring(String nickname) {
        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> NicknamePolicy.normalizeAndValidate(nickname)
        );

        assertEquals(ErrorCode.RESERVED_NICKNAME, exception.getErrorCode());
    }

    @Test
    void rejectsFullWidthDeletedAfterUnicodeNormalization() {
        assertThrows(
                GeneralException.class,
                () -> NicknamePolicy.normalizeAndValidate("ＤＥＬＥＴＥＤ_USER")
        );
    }

    @Test
    void acceptsAndTrimsOrdinaryNickname() {
        assertEquals("새사용자", NicknamePolicy.normalizeAndValidate("  새사용자  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"한", "abcdefghijklmnopqrstu", "닉네임!"})
    void rejectsNicknameOutsideSharedLengthAndCharacterPolicy(String nickname) {
        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> NicknamePolicy.normalizeAndValidate(nickname)
        );

        assertEquals(ErrorCode.INVALID_NICKNAME, exception.getErrorCode());
    }
}
