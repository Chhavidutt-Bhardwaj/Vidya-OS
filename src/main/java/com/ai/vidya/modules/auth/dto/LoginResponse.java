package com.ai.vidya.modules.auth.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Set;
import java.util.UUID;

@Getter @Builder
public class LoginResponse {
    private String  accessToken;
    private String  refreshToken;
    private String  tokenType;
    private long    expiresIn;
    private UUID    userId;
    private String  userType;
    private UUID    schoolId;
    private String  schoolName;
    private UUID    chainId;
    private String  chainName;
    private String  fullName;
    private Set<String> roles;
    private Set<String> permissions;
}
