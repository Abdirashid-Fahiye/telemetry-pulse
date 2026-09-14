package fahiye.telemetry.ports;

import fahiye.telemetry.domain.VehicleState;

public interface VehicleStateRepository {
    void saveState(VehicleState state);
    VehicleState findState(String vehicleId);
}
