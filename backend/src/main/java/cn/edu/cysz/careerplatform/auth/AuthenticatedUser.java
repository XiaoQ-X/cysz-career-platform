package cn.edu.cysz.careerplatform.auth;

import java.util.UUID;

import cn.edu.cysz.careerplatform.user.UserRole;

public record AuthenticatedUser(UUID id, String username, String displayName, UserRole role, int tokenVersion) {
}
