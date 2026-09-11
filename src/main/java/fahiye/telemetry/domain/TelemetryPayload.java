package fahiye.telemetry.domain;

import java.time.Instant;

public record TelemetryPayload(
        String vehicleId,
        Instant timestamp,
        double speedKmh,
        double engineTempCelsius,
        double batterySoC,
        double latitude,
        double longitude
) {

    // Stub: Will throw an exception if data is invalid
    public void validate() {
        // TODO: Implement logic in Issue #4
    }

    // Stub: Returns true if engine temp > 105.0 C
    public boolean hasOverheatingRisk() {
        return false; // Dummy value for TDD
    }

    // Stub: Returns true if battery < 10.0 %
    public boolean hasCriticalBattery() {
        return false; // Dummy value for TDD
    }
}
