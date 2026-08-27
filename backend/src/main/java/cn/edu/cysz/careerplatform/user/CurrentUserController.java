package cn.edu.cysz.careerplatform.user;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.cysz.careerplatform.auth.AuthenticatedUser;
import cn.edu.cysz.careerplatform.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/users")
public class CurrentUserController {

	@GetMapping("/me")
	ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal AuthenticatedUser user,
			HttpServletRequest request) {
		return ApiResponse.of(new CurrentUserResponse(user.id(), user.username(), user.displayName(), user.role()),
				(String) request.getAttribute("traceId"));
	}

	public record CurrentUserResponse(UUID id, String username, String displayName, UserRole role) {
	}
}
