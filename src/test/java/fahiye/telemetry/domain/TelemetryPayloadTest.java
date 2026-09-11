package fahiye.telemetry.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryPayloadTest {

    @Test
    void shouldCreateValidPayloadWithoutExceptions() {
        // Arrange
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-1001",
                Instant.now(),
                85.5,       // speed
                90.0,       // normal temp
                75.0,       // normal battery
                -26.2041,   // latitude
                28.0473     // longitude
        );

        // Act & Assert
        assertDoesNotThrow(payload::validate, "A valid payload should not throw an exception");
        assertFalse(payload.hasOverheatingRisk());
        assertFalse(payload.hasCriticalBattery());
    }

    @Test
    void validateShouldThrowExceptionForInvalidLatitude() {
        // Arrange: Latitude must be between -90 and 90
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-1001", Instant.now(), 50.0, 90.0, 75.0,
                -100.0, // Invalid latitude
                28.0473
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                payload::validate
        );
        assertTrue(exception.getMessage().contains("Latitude"), "Error message should mention Latitude");
    }

    @Test
    void validateShouldThrowExceptionForNegativeSpeed() {
        // Arrange
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-1001", Instant.now(),
                -10.0, // Invalid speed
                90.0, 75.0, -26.2041, 28.0473
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, payload::validate);
    }

    @Test
    void shouldDetectOverheatingRisk() {
        // Arrange: Temp > 105.0 is considered overheating
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-1001", Instant.now(), 120.0,
                106.5, // Overheating
                50.0, -26.2041, 28.0473
        );

        // Act & Assert
        assertTrue(payload.hasOverheatingRisk(), "Should return true when temp > 105.0");
    }

    @Test
    void shouldDetectCriticalBattery() {
        // Arrange: Battery < 10.0 is critical
        TelemetryPayload payload = new TelemetryPayload(
                "VEH-1001", Instant.now(), 50.0, 90.0,
                8.0, // Critical battery
                -26.2041, 28.0473
        );

        // Act & Assert
        assertTrue(payload.hasCriticalBattery(), "Should return true when battery < 10.0");
    }
}
