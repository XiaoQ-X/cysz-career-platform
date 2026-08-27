package cn.edu.cysz.careerplatform.auth;

import java.util.UUID;

import cn.edu.cysz.careerplatform.user.UserRole;

public interface IdentityProvider {

	AuthenticatedIdentity authenticate(String username, char[] password);

	record AuthenticatedIdentity(UUID userId, String username, String displayName, UserRole role) {
	}

	final class InvalidCredentialsException extends RuntimeException {

		public InvalidCredentialsException() {
			super("Invalid credentials");
		}
	}
}
