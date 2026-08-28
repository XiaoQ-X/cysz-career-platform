package cn.edu.cysz.careerplatform.auth;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

	Optional<RefreshSession> findByTokenHash(String tokenHash);

	long countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(java.util.UUID userId, java.time.Instant now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from RefreshSession session where session.tokenHash = :tokenHash")
	Optional<RefreshSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
