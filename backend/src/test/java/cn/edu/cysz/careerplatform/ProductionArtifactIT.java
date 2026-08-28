package cn.edu.cysz.careerplatform;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionArtifactIT {

	private static final List<String> FORBIDDEN_ENTRIES = List.of(
			"BOOT-INF/classes/application-e2e.yaml",
			"BOOT-INF/classes/cn/edu/cysz/careerplatform/auth/E2eAccountsConfiguration.class");

	private static final List<String> FORBIDDEN_CONTENT = List.of(
			"Student123!",
			"Teacher123!",
			"Admin123!",
			"task10-e2e-signing-secret-32-bytes-minimum");

	@Test
	void productionJarExcludesE2eProfilesCredentialsAndFixtureCode() throws Exception {
		Path artifact = Path.of(System.getProperty("production.jar"));
		List<String> entryNames = new ArrayList<>();
		StringBuilder unpackedText = new StringBuilder();

		try (JarFile jar = new JarFile(artifact.toFile())) {
			var entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				entryNames.add(entry.getName());
				if (!entry.isDirectory() && entry.getName().startsWith("BOOT-INF/classes/")) {
					try (InputStream input = jar.getInputStream(entry)) {
						unpackedText.append(new String(input.readAllBytes(), StandardCharsets.ISO_8859_1));
					}
				}
			}
		}

		assertThat(entryNames).doesNotContainAnyElementsOf(FORBIDDEN_ENTRIES);
		assertThat(unpackedText.toString()).doesNotContain(FORBIDDEN_CONTENT.toArray(String[]::new));
	}
}
