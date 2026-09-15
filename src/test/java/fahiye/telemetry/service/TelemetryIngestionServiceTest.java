package fahiye.telemetry.service;

import fahiye.telemetry.domain.TelemetryPayload;
import fahiye.telemetry.domain.VehicleState;
import fahiye.telemetry.ports.QueueConsumer;
import fahiye.telemetry.ports.TelemetryArchiveStorage;
import fahiye.telemetry.ports.VehicleStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {

    // 1. Create fake versions of our external cloud ports
    @Mock
    private QueueConsumer queueConsumer;

    @Mock
    private VehicleStateRepository stateRepository;

    @Mock
    private TelemetryArchiveStorage archiveStorage;

    private TelemetryIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        // Inject the fakes into our real service before every test
        ingestionService = new TelemetryIngestionService(queueConsumer, stateRepository, archiveStorage);
    }

    @Test
    void processIngestionBatch_WithNoMessages_ReturnsZero() {
        // Arrange: Tell the fake queue to return an empty list
        when(queueConsumer.pollBatch(anyInt())).thenReturn(Collections.emptyList());

        // Act
        int processedCount = ingestionService.processIngestionBatch();

        // Assert: Ensure it returns 0 and NEVER calls the database or storage
        assertEquals(0, processedCount);
        verify(stateRepository, never()).saveState(any());
        verify(archiveStorage, never()).archiveBatch(any());
    }

    @Test
    void processIngestionBatch_WithValidMessages_SavesStateAndArchives() {
        // Arrange: Give the fake queue one valid payload
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-2002", Instant.now(), 100.0, 90.0, 80.0, -26.2041, 28.0473
        );
        when(queueConsumer.pollBatch(anyInt())).thenReturn(List.of(payload));

        // Act
        int processedCount = ingestionService.processIngestionBatch();

        // Assert
        assertEquals(1, processedCount, "Should successfully process 1 message");

        // Verify the Hot Data was sent to the repository
        ArgumentCaptor<VehicleState> stateCaptor = ArgumentCaptor.forClass(VehicleState.class);
        verify(stateRepository, times(1)).saveState(stateCaptor.capture());
        assertEquals("VEH-2002", stateCaptor.getValue().vehicleId());

        // Verify the Cold Data was sent to the archive
        verify(archiveStorage, times(1)).archiveBatch(List.of(payload));
    }

    @Test
    void processIngestionBatch_WithInvalidMessage_SkipsInvalidAndProcessesValid() {
        // Arrange: One valid payload and one corrupted payload (negative speed)
        TelemetryPayload validPayload = new TelemetryPayload(
                "VEH-2002", Instant.now(), 100.0, 90.0, 80.0, -26.2041, 28.0473
        );
        TelemetryPayload invalidPayload = new TelemetryPayload(
                "VEH-ERROR", Instant.now(), -50.0, 90.0, 80.0, -26.2041, 28.0473
        );

        when(queueConsumer.pollBatch(anyInt())).thenReturn(List.of(validPayload, invalidPayload));

        // Act
        int processedCount = ingestionService.processIngestionBatch();

        // Assert: The service should catch the domain error, skip the bad data, and process the good one.
        assertEquals(1, processedCount, "Should only count valid processed payloads");
        verify(stateRepository, times(1)).saveState(any(VehicleState.class));
        verify(archiveStorage, times(1)).archiveBatch(List.of(validPayload));
    }
}
