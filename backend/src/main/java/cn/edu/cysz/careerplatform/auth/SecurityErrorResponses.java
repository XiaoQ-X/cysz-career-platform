package cn.edu.cysz.careerplatform.auth;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import cn.edu.cysz.careerplatform.common.web.TraceIdFilter;

final class SecurityErrorResponses {

	private SecurityErrorResponses() {
	}

	static void unauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
		write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required");
	}

	static void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
		write(request, response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access denied");
	}

	private static void write(HttpServletRequest request, HttpServletResponse response, int status, String code,
			String message) throws IOException {
		String traceId = (String) request.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE);
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
			request.setAttribute(TraceIdFilter.REQUEST_ATTRIBUTE, traceId);
		}
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(TraceIdFilter.RESPONSE_HEADER, traceId);
		response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
				+ "\",\"fieldErrors\":{},\"traceId\":\"" + traceId + "\"}");
	}
}
