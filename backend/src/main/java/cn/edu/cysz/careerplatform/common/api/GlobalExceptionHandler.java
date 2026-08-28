package cn.edu.cysz.careerplatform.common.api;

import jakarta.servlet.http.HttpServletRequest;
import cn.edu.cysz.careerplatform.auth.AuthService;
import cn.edu.cysz.careerplatform.auth.AuthRequestPolicy;
import cn.edu.cysz.careerplatform.auth.IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

	@ExceptionHandler(AuthRequestPolicy.RequestRejectedException.class)
	ResponseEntity<ApiError> handleRejectedAuthRequest(AuthRequestPolicy.RequestRejectedException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
				"AUTH_REQUEST_REJECTED", "Authentication request rejected", Map.of(), traceId(request)));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		String traceId = traceId(request);
		LOGGER.error("Unhandled exception traceId={} type={} location={}", traceId,
				exception.getClass().getName(), sanitizedLocation(exception));
		ApiError error = new ApiError(
				"INTERNAL_ERROR",
				"Internal server error",
				Map.of(),
				traceId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	private String traceId(HttpServletRequest request) {
		return (String) request.getAttribute("traceId");
	}

	private String sanitizedLocation(Exception exception) {
		StackTraceElement[] stack = exception.getStackTrace();
		return stack.length == 0 ? "unknown" : stack[0].toString();
	}
}
