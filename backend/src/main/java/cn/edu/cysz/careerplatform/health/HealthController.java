package cn.edu.cysz.careerplatform.health;

import cn.edu.cysz.careerplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	private final ReadinessProbe readinessProbe;

	public HealthController(ReadinessProbe readinessProbe) {
		this.readinessProbe = readinessProbe;
	}

	@GetMapping
	ResponseEntity<ApiResponse<Map<String, String>>> health(HttpServletRequest request) {
		boolean ready = readinessProbe.isReady();
		ApiResponse<Map<String, String>> body = ApiResponse.of(
				Map.of("status", ready ? "UP" : "OUT_OF_SERVICE"),
				(String) request.getAttribute("traceId"));
		return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
	}
}
