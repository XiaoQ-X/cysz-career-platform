package cn.edu.cysz.careerplatform.auth;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(@Value("${app.auth.jwt-secret}") String signingSecret,
			cn.edu.cysz.careerplatform.user.UserAccountRepository users) {
		return new JwtAuthenticationFilter(signingSecret, users);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.ignoringRequestMatchers(
						"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout"))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								SecurityErrorResponses.unauthenticated(request, response))
						.accessDeniedHandler((request, response, exception) ->
								SecurityErrorResponses.forbidden(request, response)))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/v1/teacher/**").hasRole("TEACHER")
						.requestMatchers("/api/v1/student/**").hasRole("STUDENT")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
