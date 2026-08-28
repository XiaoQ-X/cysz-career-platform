package cn.edu.cysz.careerplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CareerPlatformApplicationTests {

	@Container
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void doesNotRegisterSpringBootsGeneratedInMemoryUser() {
		assertTrue(applicationContext.getBeansOfType(InMemoryUserDetailsManager.class).isEmpty(),
				"the bootstrap application must not register Spring Boot's generated default user");
	}

}
