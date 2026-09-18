package fahiye.telemetry;

import fahiye.telemetry.adapters.aws.DynamoDbStateRepository;
import fahiye.telemetry.adapters.aws.S3ArchiveStorage;
import fahiye.telemetry.adapters.aws.SqsQueueConsumer;
import fahiye.telemetry.domain.VehicleState;
import fahiye.telemetry.service.TelemetryIngestionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TelemetryPulseIntegrationTest {

    private static SqsClient sqsClient;
    private static DynamoDbClient dynamoDbClient;
    private static S3Client s3Client;

    private static String queueUrl;
    private static final String TABLE_NAME = "VehicleStateTable";
    private static final String BUCKET_NAME = "telemetry-archive-bucket";

    @BeforeAll
    static void setupLocalStack() {
        // 1. Point all AWS SDK clients to our local Docker container (Port 4566)
        URI localstackUri = URI.create("http://localhost:4566");
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
        );
        Region region = Region.AF_SOUTH_1;

        sqsClient = SqsClient.builder().endpointOverride(localstackUri).credentialsProvider(credentials).region(region).build();
        dynamoDbClient = DynamoDbClient.builder().endpointOverride(localstackUri).credentialsProvider(credentials).region(region).build();
        s3Client = S3Client.builder().endpointOverride(localstackUri).credentialsProvider(credentials).region(region).forcePathStyle(true).build();

        // 2. Create the Cloud Infrastructure inside LocalStack
        queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName("test-queue").build()).queueUrl();

        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName("vehicleId").keyType(KeyType.HASH).build())
                .attributeDefinitions(AttributeDefinition.builder().attributeName("vehicleId").attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build())
                .build());

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }

    @Test
    void fullPipelineIntegrationTest() {
        // 3. Instantiate the Adapters (This makes the grey classes active!)
        SqsQueueConsumer consumer = new SqsQueueConsumer(sqsClient, queueUrl);
        DynamoDbStateRepository stateRepo = new DynamoDbStateRepository(dynamoDbClient, TABLE_NAME);
        S3ArchiveStorage archiveStorage = new S3ArchiveStorage(s3Client, BUCKET_NAME);

        // 4. Inject Adapters into the Manager
        TelemetryIngestionService service = new TelemetryIngestionService(consumer, stateRepo, archiveStorage);

        // 5. Simulate a connected vehicle firing JSON telemetry directly into the AWS Queue
        String vehicleJson = """
                {
                  "vehicleId": "VEH-9999",
                  "timestamp": "%s",
                  "speedKmh": 120.5,
                  "engineTempCelsius": 90.0,
                  "batterySoC": 85.0,
                  "latitude": -26.2041,
                  "longitude": 28.0473
                }
                """.formatted(Instant.now().toString());

        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(vehicleJson).build());

        // 6. Execute the business logic orchestration
        int processedCount = service.processIngestionBatch();

        // 7. Verify the data traversed the entire Hexagonal architecture and hit the database
        assertEquals(1, processedCount, "Service should process exactly 1 message from the queue");
        VehicleState savedState = stateRepo.findState("VEH-9999");

        assertNotNull(savedState, "DynamoDB should contain the vehicle state");
        assertEquals(120.5, savedState.currentSpeed(), "Speed should match the ingested data");
        assertEquals("OK", savedState.status(), "Status should be OK");
    }
}