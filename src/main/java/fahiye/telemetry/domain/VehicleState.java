package fahiye.telemetry.domain;

import java.time.Instant;

public record VehicleState(
        String vehicleId,
        Instant lastUpdated,
        double currentSpeed,
        double currentTemp,
        double currentBattery,
        String status
) {}
