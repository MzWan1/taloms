package za.co.taloms.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.taloms.security.domain.entity.Role;
import za.co.taloms.security.domain.entity.User;

import java.util.Collections;
import java.util.Set;

public class SecurityTestUtils {

    public static void authenticateAs(String username, String role) {
        var authorities = Collections.singleton(new SimpleGrantedAuthority(role));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void authenticateAsAdmin() {
        authenticateAs("admin", "ROLE_SYSTEM_ADMIN");
    }

    public static void authenticateAsTAAdmin() {
        authenticateAs("ta_admin", "ROLE_TA_ADMINISTRATOR");
    }

    public static void authenticateAsLandOfficer() {
        authenticateAs("land_officer", "ROLE_LAND_OFFICER");
    }

    public static void authenticateAsDataCapturer() {
        authenticateAs("data_capturer", "ROLE_DATA_CAPTURER");
    }

    public static void authenticateAsReportViewer() {
        authenticateAs("report_viewer", "ROLE_REPORT_VIEWER");
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    public static User createTestUser(String username, String role) {
        var userRole = Role.builder()
                .name(role)
                .build();

        return User.builder()
                .username(username)
                .email(username + "@test.com")
                .fullName("Test User")
                .enabled(true)
                .accountLocked(false)
                .roles(Set.of(userRole))
                .build();
    }
}