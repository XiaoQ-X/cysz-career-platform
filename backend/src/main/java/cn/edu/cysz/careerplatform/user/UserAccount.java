package cn.edu.cysz.careerplatform.user;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_account")
public class UserAccount {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(nullable = false, length = 64)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserRole role;

	@Column(nullable = false, length = 16)
	private String status = "ACTIVE";

	@Column(name = "token_version", nullable = false)
	private int tokenVersion;

	protected UserAccount() {
	}

	private UserAccount(String username, String passwordHash, String displayName, UserRole role) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
	}

	public static UserAccount create(String username, String passwordHash, String displayName, UserRole role) {
		return new UserAccount(username, passwordHash, displayName, role);
	}

	public UserRole getRole() {
		return role;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getTokenVersion() {
		return tokenVersion;
	}

	public boolean isActive() {
		return "ACTIVE".equals(status);
	}
}
