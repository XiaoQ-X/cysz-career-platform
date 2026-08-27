package cn.edu.cysz.careerplatform.common.api;

import jakarta.servlet.http.HttpServletRequest;
import cn.edu.cysz.careerplatform.auth.AuthService;
import cn.edu.cysz.careerplatform.auth.IdentityProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(fieldError.getField(), "Invalid value");
		}

		ApiError error = new ApiError(
				"VALIDATION_FAILED",
				"Validation failed",
				fieldErrors,
				(String) request.getAttribute("traceId"));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IdentityProvider.InvalidCredentialsException.class)
	ResponseEntity<ApiError> handleInvalidCredentials(IdentityProvider.InvalidCredentialsException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
				"INVALID_CREDENTIALS", "Invalid credentials", Map.of(), traceId(request)));
	}

	@ExceptionHandler(AuthService.InvalidRefreshTokenException.class)
	ResponseEntity<ApiError> handleInvalidRefreshToken(AuthService.InvalidRefreshTokenException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
				"INVALID_REFRESH_TOKEN", "Invalid refresh token", Map.of(), traceId(request)));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		ApiError error = new ApiError(
				"INTERNAL_ERROR",
				"Internal server error",
				Map.of(),
				(String) request.getAttribute("traceId"));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	private String traceId(HttpServletRequest request) {
		return (String) request.getAttribute("traceId");
	}
}
