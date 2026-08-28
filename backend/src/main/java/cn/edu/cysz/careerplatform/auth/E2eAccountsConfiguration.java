package cn.edu.cysz.careerplatform.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;
import cn.edu.cysz.careerplatform.user.UserRole;

@Configuration(proxyBeanMethods = false)
@Profile("e2e")
class E2eAccountsConfiguration {

	@Bean
	CommandLineRunner e2eAccounts(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
		return args -> {
			seed(repository, passwordEncoder, "student", "Student123!", "张同学", UserRole.STUDENT);
			seed(repository, passwordEncoder, "teacher", "Teacher123!", "王老师", UserRole.TEACHER);
			seed(repository, passwordEncoder, "admin", "Admin123!", "管理员", UserRole.ADMIN);
		};
	}

	private void seed(UserAccountRepository repository, PasswordEncoder passwordEncoder,
			String username, String password, String displayName, UserRole role) {
		if (repository.findByUsernameIgnoreCase(username).isPresent()) {
			return;
		}
		repository.save(UserAccount.create(username, passwordEncoder.encode(password), displayName, role));
	}
}
