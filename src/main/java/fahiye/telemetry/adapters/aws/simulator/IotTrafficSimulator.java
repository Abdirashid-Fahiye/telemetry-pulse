package fahiye.telemetry.adapters.aws.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fahiye.telemetry.domain.TelemetryPayload;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Random;

public class IotTrafficSimulator {

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/test-queue";
    private static final Random RANDOM = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting IoT Traffic Simulator...");

        SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.AF_SOUTH_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        // Simulate 10 vehicles sending data
        for (int i = 1; i <= 10; i++) {
            TelemetryPayload payload = generateRandomPayload(i);

            try {
                String json = MAPPER.writeValueAsString(payload);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .messageBody(json)
                        .build());

                System.out.println("🚗 Sent telemetry for Vehicle " + payload.vehicleId());
            } catch (JsonProcessingException e) {
                System.err.println("Failed to serialize payload: " + e.getMessage());
            }

            // Wait half a second between emissions
            Thread.sleep(500);
        }

        System.out.println("✅ Simulator finished transmitting 10 payloads.");
    }

    private static TelemetryPayload generateRandomPayload(int id) {
        return new TelemetryPayload(
                "VEH-" + (1000 + id),
                Instant.now(),
                60.0 + (RANDOM.nextDouble() * 60.0), // Speed 60-120 km/h
                85.0 + (RANDOM.nextDouble() * 20.0), // Temp 85-105 C
                RANDOM.nextDouble() * 100.0,         // Battery 0-100%
                -26.2041 + (RANDOM.nextDouble() * 0.1), // Lat near JHB
                28.0473 + (RANDOM.nextDouble() * 0.1)   // Lon near JHB
        );
    }
}
