package fahiye.telemetry.service;

import fahiye.telemetry.domain.TelemetryPayload;
import fahiye.telemetry.domain.VehicleState;
import fahiye.telemetry.ports.QueueConsumer;
import fahiye.telemetry.ports.TelemetryArchiveStorage;
import fahiye.telemetry.ports.VehicleStateRepository;

import java.util.ArrayList;
import java.util.List;

public class TelemetryIngestionService {
    private final QueueConsumer consumer;
    private final VehicleStateRepository stateRepo;
    private final TelemetryArchiveStorage archiveStorage;

    public TelemetryIngestionService(
            QueueConsumer consumer,
            VehicleStateRepository stateRepo,
            TelemetryArchiveStorage archiveStorage) {
        this.consumer = consumer;
        this.stateRepo = stateRepo;
        this.archiveStorage = archiveStorage;
    }

    public int processIngestionBatch() {
        // 1. Poll a batch of up to 10 messages from the queue
        List<TelemetryPayload> payloads = consumer.pollBatch(10);

        if (payloads == null || payloads.isEmpty()) {
            return 0; // Nothing to process
        }

        List<TelemetryPayload> validPayloads = new ArrayList<>();

        // 2. Process each payload individually
        for (TelemetryPayload payload : payloads) {
            try {
                // This throws an exception if the data is corrupted
                payload.validate();

                // Determine vehicle status based on our domain rules
                String status = (payload.hasOverheatingRisk() || payload.hasCriticalBattery())
                        ? "ALERT" : "OK";

                // 3. Map to Hot Data State and save to repository
                VehicleState state = new VehicleState(
                        payload.vehicleId(),
                        payload.timestamp(),
                        payload.speedKmh(),
                        payload.engineTempCelsius(),
                        payload.batterySoC(),
                        status
                );

                stateRepo.saveState(state);

                // Add to our list of valid payloads for archiving
                validPayloads.add(payload);

            } catch (IllegalArgumentException e) {
                // If corrupted, we catch the error, drop the payload, and keep processing the rest
                System.err.println("Dropped invalid telemetry payload: " + e.getMessage());
            }
        }

        // 4. Archive all successfully validated payloads
        if (!validPayloads.isEmpty()) {
            archiveStorage.archiveBatch(validPayloads);
        }

        return validPayloads.size();
    }
}