package cn.edu.cysz.careerplatform.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.edu.cysz.careerplatform.auth.IdentityProvider.AuthenticatedIdentity;

@Service
public class JwtTokenService {

	public static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

	private final byte[] signingSecret;

	public JwtTokenService(@Value("${app.auth.jwt-secret}") String signingSecret) {
		this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
		if (this.signingSecret.length < 32) {
			throw new IllegalStateException("JWT signing key must contain at least 256 bits");
		}
	}

	public String issueAccessToken(AuthenticatedIdentity identity, int tokenVersion, Instant issuedAt) {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(identity.userId().toString())
				.claim("username", identity.username())
				.claim("role", identity.role().name())
				.claim("tokenVersion", tokenVersion)
				.issueTime(Date.from(issuedAt))
				.expirationTime(Date.from(issuedAt.plus(ACCESS_TOKEN_LIFETIME)))
				.build();

		try {
			JWSSigner signer = new MACSigner(signingSecret);
			SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			token.sign(signer);
			return token.serialize();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to issue access token", exception);
		}
	}
}
