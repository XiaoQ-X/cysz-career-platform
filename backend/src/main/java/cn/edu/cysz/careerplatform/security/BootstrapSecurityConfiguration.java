package cn.edu.cysz.careerplatform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class BootstrapSecurityConfiguration {

	@Bean
	UserDetailsService bootstrapUserDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Local authentication is not configured");
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	Clock utcClock() {
		return Clock.systemUTC();
	}

}
