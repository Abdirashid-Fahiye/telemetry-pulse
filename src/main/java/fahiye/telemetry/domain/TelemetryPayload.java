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

    // Executes strict boundary checks on the payload
    public void validate() {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Vehicle ID cannot be null or empty");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
        }
        if (speedKmh < 0.0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        if (batterySoC < 0.0 || batterySoC > 100.0) {
            throw new IllegalArgumentException("Battery SoC must be between 0.0 and 100.0");
        }
    }

    // Domain rules for alerts
    public boolean hasOverheatingRisk() {
        return engineTempCelsius > 105.0;
    }

    public boolean hasCriticalBattery() {
        return batterySoC >= 0.0 && batterySoC < 10.0;
    }
}
