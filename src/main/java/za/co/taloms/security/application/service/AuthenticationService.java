package za.co.taloms.security.application.service;

import za.co.taloms.security.application.dto.LoginRequest;
import za.co.taloms.security.application.dto.LoginResponse;

public interface AuthenticationService {
    LoginResponse login(LoginRequest request);
}