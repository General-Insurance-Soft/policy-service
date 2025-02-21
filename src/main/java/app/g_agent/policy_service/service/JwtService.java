package app.g_agent.policy_service.service;

import java.security.SignatureException;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtService {

	private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

	@Value("${security.jwt.secret-key}")
	private String secretKey;

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = this.extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	// "user-id
	// "organization-id
	// "authorities
	public Object getTokenValue(String token, String key) {
		logger.info("Extract JWT value: "+ token);
		Claims claims = this.extractAllClaims(token);
		return claims.get(key);

	}

	public String getJWT(HttpServletRequest request) throws Exception {
		final String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new Exception("No JWT token passed");
		}
		String jwt = authHeader.substring(7);
		logger.info("The JWT string is ===============> " + jwt);
		return jwt;

	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
		} catch (ExpiredJwtException e) {
			throw new TokenExpiredException("Token has expired");
		} catch (MalformedJwtException e) {
			throw new MalformedTokenException("Malformed token");
		} catch (Exception e) {
			throw new InvalidTokenException("Invalid token");
		}
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes); // This returns a SecretKey
	}

	private class TokenExpiredException extends RuntimeException {
		public TokenExpiredException(String message) {
			super(message);
		}
	}

	private class MalformedTokenException extends RuntimeException {
		public MalformedTokenException(String message) {
			super(message);
		}
	}

	private class InvalidTokenSignatureException extends RuntimeException {
		public InvalidTokenSignatureException(String message) {
			super(message);
		}
	}

	private class InvalidTokenException extends RuntimeException {
		public InvalidTokenException(String message) {
			super(message);
		}
	}

}
