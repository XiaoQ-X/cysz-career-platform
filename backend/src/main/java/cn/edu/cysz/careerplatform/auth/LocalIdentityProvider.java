package cn.edu.cysz.careerplatform.auth;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;

@Service
public class LocalIdentityProvider implements IdentityProvider {

	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final String dummyPasswordHash;

	public LocalIdentityProvider(UserAccountRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Override
	public AuthenticatedIdentity authenticate(String username, char[] password) {
		String normalizedUsername = username == null ? "" : username.trim();
		UserAccount user = users.findByUsernameIgnoreCase(normalizedUsername).orElse(null);
		String presentedPassword = password == null ? "" : new String(password);
		boolean passwordMatches = passwordEncoder.matches(presentedPassword,
				user == null ? dummyPasswordHash : user.getPasswordHash());

		if (user == null || !passwordMatches || !user.isActive()) {
			throw new InvalidCredentialsException();
		}

		return new AuthenticatedIdentity(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
	}
}
