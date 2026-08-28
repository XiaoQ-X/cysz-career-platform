package cn.edu.cysz.careerplatform.user;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountSerializationTest {

	@Test
	void passwordHashIsNeverPartOfJacksonSerialization() throws Exception {
		String passwordHash = "$2a$10$serialization-regression-secret";
		UserAccount account = UserAccount.create("student", passwordHash, "张同学", UserRole.STUDENT);

		String json = new ObjectMapper().writeValueAsString(account);

		assertThat(json)
				.doesNotContain("passwordHash")
				.doesNotContain(passwordHash);
	}
}
