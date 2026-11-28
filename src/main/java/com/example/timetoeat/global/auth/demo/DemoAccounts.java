package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.global.auth.model.provider.KakaoUser;

/** 시연 전용 회원(1~5). 이메일은 코드에 고정돼 있어 요청으로 다른 회원을 고를 수 없다. */
public final class DemoAccounts {
    private DemoAccounts() {}

    public static String email(int account) {
        return "qa_demo" + account + "@" + KakaoUser.SYNTHETIC_EMAIL_DOMAIN;
    }

    /** 화면에 보이는 이름. 시연 회원 구분은 사용자에게 보이지 않는 이메일로만 한다. */
    static final java.util.List<String> NICKNAMES = java.util.List.of("김민준", "최서연", "박지윤", "강태민", "이소율");

    public static String nickname(int account) {
        return NICKNAMES.get(account - 1);
    }

    public static boolean valid(Integer account) {
        return account != null && account >= 1 && account <= DemoLoginProperties.ACCOUNT_COUNT;
    }
}
