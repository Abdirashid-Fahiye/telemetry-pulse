package fahiye.telemetry.adapters.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fahiye.telemetry.domain.TelemetryPayload;
import fahiye.telemetry.ports.QueueConsumer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;

public class SqsQueueConsumer implements QueueConsumer {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public SqsQueueConsumer(SqsClient sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;

        // Jackson Object Mapper converts JSON text into our Java Records
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Needed for Java Instant (timestamps)
    }

    @Override
    public List<TelemetryPayload> pollBatch(int batchSize) {
        // 1. Build the request to ask AWS for messages
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(Math.min(batchSize, 10)) // AWS max per batch is 10
                .waitTimeSeconds(5) // Long polling prevents burning CPU cycles
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
        List<TelemetryPayload> payloads = new ArrayList<>();

        // 2. Loop through the raw AWS messages
        for (Message message : messages) {
            try {
                // Translate the JSON body into our TelemetryPayload record
                TelemetryPayload payload = objectMapper.readValue(message.body(), TelemetryPayload.class);
                payloads.add(payload);

                // 3. Delete the message from the queue so we don't process it twice
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());

            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse message JSON: " + e.getMessage());
            }
        }

        return payloads;
    }
}