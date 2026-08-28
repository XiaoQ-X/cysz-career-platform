package cn.edu.cysz.careerplatform.health;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseReadinessProbeTest {

	@Mock
	private ApplicationAvailability availability;

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection connection;

	@Test
	void remainsUnavailableUntilApplicationRunnersHaveCompleted() throws Exception {
		when(availability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);

		assertThat(new DatabaseReadinessProbe(availability, dataSource).isReady()).isFalse();

		verify(dataSource, never()).getConnection();
	}

	@Test
	void remainsUnavailableWhenTheDatabaseCannotValidateAConnection() throws Exception {
		when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(2)).thenReturn(false);

		assertThat(new DatabaseReadinessProbe(availability, dataSource).isReady()).isFalse();
	}

	@Test
	void remainsUnavailableWhenOpeningTheDatabaseConnectionFails() throws Exception {
		when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
		when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));

		assertThat(new DatabaseReadinessProbe(availability, dataSource).isReady()).isFalse();
	}

	@Test
	void becomesReadyOnlyAfterStartupAndDatabaseValidationSucceed() throws Exception {
		when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.isValid(2)).thenReturn(true);

		assertThat(new DatabaseReadinessProbe(availability, dataSource).isReady()).isTrue();
	}
}
