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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import({TraceIdFilter.class, GlobalExceptionHandler.class, ValidationProbeController.class, TestSecurityConfiguration.class})
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
	void mapsBeanValidationFailuresToThePublicErrorEnvelopeWithoutExceptionDetails() throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"))
				.andExpect(jsonPath("$.traceId").isNotEmpty())
				.andExpect(header().exists("X-Trace-Id"))
				.andReturn();

		assertEnvelopeTraceIdMatchesHeader(result);
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("MethodArgumentNotValidException", "Exception", "stackTrace");
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
}

record ValidationRequest(@NotBlank String name) {
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
