package cn.edu.cysz.careerplatform.health;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

@Component
class DatabaseReadinessProbe implements ReadinessProbe {

	private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 2;

	private final ApplicationAvailability availability;
	private final DataSource dataSource;

	DatabaseReadinessProbe(ApplicationAvailability availability, DataSource dataSource) {
		this.availability = availability;
		this.dataSource = dataSource;
	}

	@Override
	public boolean isReady() {
		if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
			return false;
		}
		try (var connection = dataSource.getConnection()) {
			return connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
		} catch (SQLException exception) {
			return false;
		}
	}
}
