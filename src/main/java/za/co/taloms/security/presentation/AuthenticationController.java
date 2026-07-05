package za.co.taloms.security.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.security.application.dto.LoginRequest;
import za.co.taloms.security.application.dto.LoginResponse;
import za.co.taloms.security.application.service.AuthenticationService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authenticationService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Login successful"));
    }

    @GetMapping("/hash")
    public ResponseEntity<String> generateHash() {
        var encoder = new BCryptPasswordEncoder(12);
        return ResponseEntity.ok(encoder.encode("Admin@1234"));
    }
}