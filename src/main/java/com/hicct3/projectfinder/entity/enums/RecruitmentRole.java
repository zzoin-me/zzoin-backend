package com.hicct3.projectfinder.entity.enums;

import java.util.Arrays;
import java.util.List;

public enum RecruitmentRole {
    SERVICE_PLANNING("서비스 기획", RecruitmentCategory.PLANNING),
    PM("PM", RecruitmentCategory.PLANNING),
    PROJECT_MANAGER("프로젝트 매니저", RecruitmentCategory.PLANNING),
    BUSINESS_PLANNING("사업 기획", RecruitmentCategory.PLANNING),

    UX("UX", RecruitmentCategory.DESIGN),
    UI("UI", RecruitmentCategory.DESIGN),
    UX_UI("UX/UI", RecruitmentCategory.DESIGN),
    GRAPHIC("그래픽", RecruitmentCategory.DESIGN),
    BRAND_DESIGN("브랜드", RecruitmentCategory.DESIGN),
    ILLUSTRATION("일러스트", RecruitmentCategory.DESIGN),

    FRONTEND("프론트엔드", RecruitmentCategory.DEVELOPMENT),
    BACKEND("백엔드", RecruitmentCategory.DEVELOPMENT),
    IOS("iOS", RecruitmentCategory.DEVELOPMENT),
    ANDROID("안드로이드", RecruitmentCategory.DEVELOPMENT),
    CROSS_PLATFORM("크로스플랫폼", RecruitmentCategory.DEVELOPMENT),
    DESKTOP("데스크탑", RecruitmentCategory.DEVELOPMENT),
    GAME_CLIENT("게임 클라이언트", RecruitmentCategory.DEVELOPMENT),
    GAME_SERVER("게임 서버", RecruitmentCategory.DEVELOPMENT),
    DEVOPS("DevOps", RecruitmentCategory.DEVELOPMENT),
    DATA_ENGINEERING("데이터 엔지니어링", RecruitmentCategory.DEVELOPMENT),
    SECURITY("보안", RecruitmentCategory.DEVELOPMENT),

    CONTENT("콘텐츠", RecruitmentCategory.MARKETING),
    GROWTH("성장", RecruitmentCategory.MARKETING),
    SNS("SNS", RecruitmentCategory.MARKETING),
    BRAND_MARKETING("브랜드", RecruitmentCategory.MARKETING),
    AD("광고", RecruitmentCategory.MARKETING),
    PR("PR", RecruitmentCategory.MARKETING);

    private final String displayName;
    private final RecruitmentCategory category;

    RecruitmentRole(String displayName, RecruitmentCategory category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RecruitmentCategory getCategory() {
        return category;
    }

    public static RecruitmentRole fromDisplayName(String name) {
        for (RecruitmentRole role : values()) {
            if (role.displayName.equals(name)) {
                return role;
            }
        }
        return null;
    }

    public static List<String> displayNamesByCategory(RecruitmentCategory category) {
        return Arrays.stream(values())
                .filter(r -> r.category == category)
                .map(RecruitmentRole::getDisplayName)
                .toList();
    }
}
