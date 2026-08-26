package cn.edu.cysz.careerplatform.health;

import cn.edu.cysz.careerplatform.common.api.GlobalExceptionHandler;
import cn.edu.cysz.careerplatform.common.web.TraceIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.ServletException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import({
		TraceIdFilter.class,
		GlobalExceptionHandler.class,
		ValidationProbeController.class,
		TraceProbeController.class,
		AsyncTraceProbeController.class,
		TestSecurityConfiguration.class
})
class HealthControllerTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void generatesAnEnvelopeTraceIdAndCleansMdcWhenTheClientSendsNone() throws Exception {
		MvcResult result = mvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UP"))
				.andExpect(jsonPath("$.traceId").isNotEmpty())
				.andExpect(header().exists("X-Trace-Id"))
				.andReturn();

		assertEnvelopeTraceIdMatchesHeader(result);
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void preservesAValidClientTraceIdInsteadOfReplacingIt() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";

		MvcResult result = mvc.perform(get("/api/v1/health").header("X-Trace-Id", traceId))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Trace-Id", traceId))
				.andExpect(jsonPath("$.traceId").value(traceId))
				.andReturn();

		assertThat(result.getResponse().getHeader("X-Trace-Id")).isEqualTo(traceId);
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void replacesAnInvalidClientTraceIdBeforeItReachesTheApiContract() throws Exception {
		MvcResult result = mvc.perform(get("/api/v1/health").header("X-Trace-Id", "untrusted-trace-id"))
				.andExpect(status().isOk())
				.andReturn();

		assertEnvelopeTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getHeader("X-Trace-Id")).isNotEqualTo("untrusted-trace-id");
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void replacesAnUppercaseUuidInsteadOfTreatingItAsCanonical() throws Exception {
		String uppercaseTraceId = "E6D9F77D-9520-4F64-B65C-0ABEF2721E5B";

		MvcResult result = mvc.perform(get("/api/v1/health").header("X-Trace-Id", uppercaseTraceId))
				.andExpect(status().isOk())
				.andReturn();

		assertEnvelopeTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getHeader("X-Trace-Id")).isNotEqualTo(uppercaseTraceId);
	}

	@Test
	void exposesAFixedSafeMessageForBeanValidationFailures() throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.name").value("Invalid value"))
				.andExpect(jsonPath("$.traceId").isNotEmpty())
				.andExpect(header().exists("X-Trace-Id"))
				.andReturn();

		assertEnvelopeTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("MethodArgumentNotValidException", "Exception", "stackTrace");
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void neverReturnsSensitiveFieldValuesOrValidationMessages() throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/test/validation/sensitive")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"password\":\"actual-password\",\"resume\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.resume").value("Invalid value"))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("actual-password", "CONFIDENTIAL_RESUME_BODY");
	}

	@Test
	void makesTheTraceIdAvailableInMdcDuringSynchronousControllerExecution() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";

		mvc.perform(get("/api/v1/test/trace").header("X-Trace-Id", traceId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.traceId").value(traceId));
	}

	@Test
	void preservesTheTraceIdAndMdcAcrossAsyncSuccessDispatch() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";

		MvcResult initial = mvc.perform(get("/api/v1/test/async/success").header("X-Trace-Id", traceId))
				.andExpect(request().asyncStarted())
				.andReturn();

		mvc.perform(asyncDispatch(initial))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Trace-Id", traceId))
				.andExpect(jsonPath("$.traceId").value(traceId));
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void preservesTheTraceIdAndCleansMdcWhenAsyncControllerFails() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";

		MvcResult initial = mvc.perform(get("/api/v1/test/async/error").header("X-Trace-Id", traceId))
				.andExpect(request().asyncStarted())
				.andReturn();

		mvc.perform(asyncDispatch(initial))
				.andExpect(status().isInternalServerError())
				.andExpect(header().string("X-Trace-Id", traceId))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.traceId").value(traceId));
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	@Test
	void removesMdcWhenTheFilterChainThrows() {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Trace-Id", traceId);

		assertThatThrownBy(() -> new TraceIdFilter().doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
			assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo(traceId);
			throw new ServletException("expected test failure");
		})).isInstanceOf(ServletException.class);
		assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
	}

	private void assertEnvelopeTraceIdMatchesHeader(MvcResult result) throws Exception {
		String responseTraceId = result.getResponse().getHeader("X-Trace-Id");
		assertThat(responseTraceId).isNotBlank();
		assertThat(UUID.fromString(responseTraceId).toString()).isEqualTo(responseTraceId);
		assertThat(result.getResponse().getContentAsString()).contains("\"traceId\":\"" + responseTraceId + "\"");
	}
}

@RestController
@RequestMapping("/api/v1/test/validation")
class ValidationProbeController {

	@PostMapping
	void validate(@Valid @RequestBody ValidationRequest request) {
	}

	@PostMapping("/sensitive")
	void validateSensitive(@Valid @RequestBody SensitiveValidationRequest request) {
	}
}

record ValidationRequest(@NotBlank String name) {
}

record SensitiveValidationRequest(
		@NotBlank String password,
		@NotBlank(message = "CONFIDENTIAL_RESUME_BODY") String resume) {
}

@RestController
@RequestMapping("/api/v1/test/trace")
class TraceProbeController {

	@GetMapping
	Map<String, String> trace() {
		return Map.of("traceId", MDC.get(TraceIdFilter.MDC_KEY));
	}
}

@RestController
@RequestMapping("/api/v1/test/async")
class AsyncTraceProbeController {

	@GetMapping("/success")
	Callable<Map<String, String>> success() {
		return () -> Map.of("traceId", String.valueOf(MDC.get(TraceIdFilter.MDC_KEY)));
	}

	@GetMapping("/error")
	Callable<Void> error() {
		return () -> {
			throw new IllegalStateException("async failure");
		};
	}
}

@TestConfiguration(proxyBeanMethods = false)
class TestSecurityConfiguration {

	@Bean
	UserDetailsService lookupOnlyUserDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Test authentication is not configured");
		};
	}

	@Bean
	SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.build();
	}
}
