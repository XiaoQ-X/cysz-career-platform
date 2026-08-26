package cn.edu.cysz.careerplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CareerPlatformApplicationTests {

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
