package fahiye.telemetry;

import fahiye.telemetry.adapters.aws.DynamoDbStateRepository;
import fahiye.telemetry.adapters.aws.S3ArchiveStorage;
import fahiye.telemetry.adapters.aws.SqsQueueConsumer;
import fahiye.telemetry.service.TelemetryIngestionService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class Application {

    // 1. Configuration via Environment Variables (Defaults to LocalStack for local testing)
    private static final String QUEUE_URL = System.getenv().getOrDefault("QUEUE_URL", "http://localhost:4566/000000000000/test-queue");
    private static final String TABLE_NAME = System.getenv().getOrDefault("TABLE_NAME", "VehicleStateTable");
    private static final String BUCKET_NAME = System.getenv().getOrDefault("BUCKET_NAME", "telemetry-archive-bucket");
    private static final String AWS_ENDPOINT = System.getenv("AWS_ENDPOINT_OVERRIDE");
    private static final Region AWS_REGION = Region.of(System.getenv().getOrDefault("AWS_REGION", "af-south-1"));

    public static void main(String[] args) {
        System.out.println("Starting TelemetryPulse Ingestion Engine...");

        // 2. Initialize Infrastructure
        SqsClient sqsClient = buildSqsClient();
        DynamoDbClient dynamoDbClient = buildDynamoDbClient();
        S3Client s3Client = buildS3Client();

        SqsQueueConsumer consumer = new SqsQueueConsumer(sqsClient, QUEUE_URL);
        DynamoDbStateRepository stateRepo = new DynamoDbStateRepository(dynamoDbClient, TABLE_NAME);
        S3ArchiveStorage archiveStorage = new S3ArchiveStorage(s3Client, BUCKET_NAME);

        TelemetryIngestionService service = new TelemetryIngestionService(consumer, stateRepo, archiveStorage);

        // 3. Graceful Shutdown Hook (Catches CTRL+C so it doesn't corrupt data during exit)
        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down TelemetryPulse gracefully...");
            running.set(false);
        }));

        // 4. The Continuous Polling Loop
        System.out.println("Engine is live and polling for telemetry data on " + QUEUE_URL);

        while (running.get()) {
            try {
                int processed = service.processIngestionBatch();

                // Back off and rest if the queue is empty to prevent burning CPU
                if (processed == 0) {
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                System.err.println("Critical error processing batch: " + e.getMessage());
                // Pause before retrying to prevent a rapid crash-loop
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // --- AWS Client Builders with LocalStack Support ---

    private static SqsClient buildSqsClient() {
        SqsClientBuilder builder = SqsClient.builder().region(AWS_REGION);
        if (AWS_ENDPOINT != null && !AWS_ENDPOINT.isEmpty()) {
            builder.endpointOverride(URI.create(AWS_ENDPOINT));
        }
        return builder.build();
    }

    private static DynamoDbClient buildDynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(AWS_REGION);
        if (AWS_ENDPOINT != null && !AWS_ENDPOINT.isEmpty()) {
            builder.endpointOverride(URI.create(AWS_ENDPOINT));
        }
        return builder.build();
    }

    private static S3Client buildS3Client() {
        S3ClientBuilder builder = S3Client.builder().region(AWS_REGION).forcePathStyle(true);
        if (AWS_ENDPOINT != null && !AWS_ENDPOINT.isEmpty()) {
            builder.endpointOverride(URI.create(AWS_ENDPOINT));
        }
        return builder.build();
    }
}
