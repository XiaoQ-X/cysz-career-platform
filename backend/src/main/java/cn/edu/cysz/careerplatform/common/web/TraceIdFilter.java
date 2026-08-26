package cn.edu.cysz.careerplatform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ATTRIBUTE = "traceId";
	public static final String MDC_KEY = "traceId";
	public static final String RESPONSE_HEADER = "X-Trace-Id";
	private static final Pattern UUID_PATTERN = Pattern.compile(
				"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = resolveTraceId(request.getHeader(RESPONSE_HEADER));
		request.setAttribute(REQUEST_ATTRIBUTE, traceId);
		response.setHeader(RESPONSE_HEADER, traceId);
		MDC.put(MDC_KEY, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	private String resolveTraceId(String suppliedTraceId) {
		if (suppliedTraceId != null && UUID_PATTERN.matcher(suppliedTraceId).matches()) {
			return suppliedTraceId;
		}
		return UUID.randomUUID().toString();
	}
}
