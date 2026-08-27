package cn.edu.cysz.careerplatform.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerLoggingTest {

	@Test
	void unexpectedExceptionLoggingKeepsTraceAndTypeButOmitsSecretMessage() {
		Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		String secret = "fake-secret-that-must-not-be-logged";
		HttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("traceId", "trace-for-log-test");

		try {
			new GlobalExceptionHandler().handleUnexpected(
					new IllegalStateException(secret), request);
		} finally {
			logger.detachAppender(appender);
		}

		String log = appender.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.collect(Collectors.joining("\n"));
		assertThat(log).contains("trace-for-log-test", IllegalStateException.class.getName(),
				"location=", "GlobalExceptionHandlerLoggingTest");
		assertThat(log).doesNotContain(secret);
	}
}
