package fahiye.telemetry.ports;

import fahiye.telemetry.domain.TelemetryPayload;
import java.util.List;

public interface TelemetryArchiveStorage {
    void archiveBatch(List<TelemetryPayload> payloads);
}
