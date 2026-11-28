package com.example.timetoeat.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.*;

/** 보안 점검 S5: Boot 3.5.15 이상(관리 버전 Tomcat 10.1.55, Spring 6.2.19, Security 6.5.11). 실수로 내려가면 실패한다. */
class DependencyFloorTest {
    private static int[] v(String s) {
        String[] p = s.replaceAll("[^0-9.].*$", "").split("\\.");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    private static boolean atLeast(String actual, String floor) {
        int[] a = v(actual), f = v(floor);
        for (int i = 0; i < 3; i++) if (a[i] != f[i]) return a[i] > f[i];
        return true;
    }

    @Test void springBootIsPatched() {
        assertTrue(atLeast(SpringBootVersion.getVersion(), "3.5.15"), SpringBootVersion.getVersion());
    }

    @Test void tomcatIsPatched() {
        String tomcat = org.apache.catalina.util.ServerInfo.getServerNumber();
        assertTrue(atLeast(tomcat, "10.1.45"), tomcat); // CVE-2025-48989(10.1.44), 10.1.45 묶음
    }
}
