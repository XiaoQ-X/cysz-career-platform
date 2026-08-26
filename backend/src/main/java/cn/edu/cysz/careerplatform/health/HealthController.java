package cn.edu.cysz.careerplatform.health;

import cn.edu.cysz.careerplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	@GetMapping
	ApiResponse<Map<String, String>> health(HttpServletRequest request) {
		return ApiResponse.of(Map.of("status", "UP"), (String) request.getAttribute("traceId"));
	}
}
