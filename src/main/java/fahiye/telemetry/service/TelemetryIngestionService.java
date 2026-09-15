package fahiye.telemetry.service;

import fahiye.telemetry.ports.QueueConsumer;
import fahiye.telemetry.ports.TelemetryArchiveStorage;
import fahiye.telemetry.ports.VehicleStateRepository;

public class TelemetryIngestionService {
    private final QueueConsumer consumer;
    private final VehicleStateRepository stateRepo;
    private final TelemetryArchiveStorage archiveStorage;

    // Dependency Injection: We inject the ports, not the concrete AWS classes
    public TelemetryIngestionService(
            QueueConsumer consumer,
            VehicleStateRepository stateRepo,
            TelemetryArchiveStorage archiveStorage) {
        this.consumer = consumer;
        this.stateRepo = stateRepo;
        this.archiveStorage = archiveStorage;
    }

    public int processIngestionBatch() {
        return 0;
    }
}
