package cn.edu.cysz.careerplatform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ATTRIBUTE = "traceId";
	public static final String MDC_KEY = "traceId";
	public static final String RESPONSE_HEADER = "X-Trace-Id";
	private static final String ASYNC_INTERCEPTOR_KEY = TraceIdFilter.class.getName();
	private static final Pattern UUID_PATTERN = Pattern.compile(
				"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = traceIdFor(request);
		request.setAttribute(REQUEST_ATTRIBUTE, traceId);
		response.setHeader(RESPONSE_HEADER, traceId);
		MDC.put(MDC_KEY, traceId);
		if (!isAsyncDispatch(request)) {
			registerAsyncMdcPropagation(request, traceId);
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	private String traceIdFor(HttpServletRequest request) {
		Object existingTraceId = request.getAttribute(REQUEST_ATTRIBUTE);
		if (existingTraceId instanceof String traceId && UUID_PATTERN.matcher(traceId).matches()) {
			return traceId;
		}
		return resolveTraceId(request.getHeader(RESPONSE_HEADER));
	}

	private void registerAsyncMdcPropagation(HttpServletRequest request, String traceId) {
		WebAsyncUtils.getAsyncManager(request).registerCallableInterceptor(ASYNC_INTERCEPTOR_KEY,
				new CallableProcessingInterceptor() {
					@Override
					public <T> void preProcess(NativeWebRequest asyncRequest, Callable<T> task) {
						MDC.put(MDC_KEY, traceId);
					}

					@Override
					public <T> void afterCompletion(NativeWebRequest asyncRequest, Callable<T> task) {
						MDC.remove(MDC_KEY);
					}
				});
	}

	private String resolveTraceId(String suppliedTraceId) {
		if (suppliedTraceId != null && UUID_PATTERN.matcher(suppliedTraceId).matches()) {
			return suppliedTraceId;
		}
		return UUID.randomUUID().toString();
	}
}
