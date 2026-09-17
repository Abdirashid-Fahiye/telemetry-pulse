package fahiye.telemetry.adapters.aws;

import fahiye.telemetry.domain.VehicleState;
import fahiye.telemetry.ports.VehicleStateRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DynamoDbStateRepository implements VehicleStateRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbStateRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void saveState(VehicleState state) {
        // Map our Java Record to AWS DynamoDB AttributeValues
        Map<String, AttributeValue> item = new HashMap<>();

        // .s() is for String, .n() is for Number
        item.put("vehicleId", AttributeValue.builder().s(state.vehicleId()).build());
        item.put("lastUpdated", AttributeValue.builder().s(state.lastUpdated().toString()).build());
        item.put("currentSpeed", AttributeValue.builder().n(String.valueOf(state.currentSpeed())).build());
        item.put("currentTemp", AttributeValue.builder().n(String.valueOf(state.currentTemp())).build());
        item.put("currentBattery", AttributeValue.builder().n(String.valueOf(state.currentBattery())).build());
        item.put("status", AttributeValue.builder().s(state.status()).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }

    @Override
    public VehicleState findState(String vehicleId) {
        // Set up the primary key to search for
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("vehicleId", AttributeValue.builder().s(vehicleId).build());

        GetItemRequest getReq = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(getReq).item();

        // If the car has never sent data before, return null
        if (item == null || item.isEmpty()) {
            return null;
        }

        // Translate the AWS AttributeValues back into our Java Record
        return new VehicleState(
                item.get("vehicleId").s(),
                Instant.parse(item.get("lastUpdated").s()),
                Double.parseDouble(item.get("currentSpeed").n()),
                Double.parseDouble(item.get("currentTemp").n()),
                Double.parseDouble(item.get("currentBattery").n()),
                item.get("status").s()
        );
    }
}
