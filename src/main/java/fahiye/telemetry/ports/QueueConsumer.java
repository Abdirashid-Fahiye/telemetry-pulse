package fahiye.telemetry.ports;

import fahiye.telemetry.domain.TelemetryPayload;
import java.util.List;

public interface QueueConsumer {
    List<TelemetryPayload> pollBatch(int batchSize);
}
