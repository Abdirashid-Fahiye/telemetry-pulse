package fahiye.telemetry.adapters.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fahiye.telemetry.domain.TelemetryPayload;
import fahiye.telemetry.ports.TelemetryArchiveStorage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class S3ArchiveStorage implements TelemetryArchiveStorage {

    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper;

    public S3ArchiveStorage(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;

        // Jackson to convert our Java List into a JSON text file
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void archiveBatch(List<TelemetryPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return;
        }

        try {
            // 1. Convert the entire list of payloads into a single JSON string
            String jsonBody = objectMapper.writeValueAsString(payloads);

            // 2. Create a folder path based on today's date (e.g., 2026/09/17)
            String datePrefix = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    .withZone(ZoneId.systemDefault())
                    .format(payloads.get(0).timestamp());

            // Generate a random unique file name so we don't overwrite previous batches
            String fileName = datePrefix + "/batch-" + UUID.randomUUID() + ".json";

            // 3. Build the request to upload the file to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/json")
                    .build();

            // 4. Send it to the cloud (or LocalStack in our case)
            s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonBody));

            System.out.println("Successfully archived batch of " + payloads.size() + " payloads to S3: " + fileName);

        } catch (JsonProcessingException e) {
            System.err.println("Failed to format payloads to JSON for S3: " + e.getMessage());
        }
    }
}
