package ni.edu.uni.aso.core;

/**
 * Representa una petición de Entrada/Salida generada en el sistema operativo.
 */
public class IORequest {
    private String id;
    private String targetDeviceId;
    private int payloadSize;
    private OperationType operationType;

    public IORequest() {
    }

    public IORequest(String id, String targetDeviceId, int payloadSize, OperationType operationType) {
        this.id = id;
        this.targetDeviceId = targetDeviceId;
        this.payloadSize = payloadSize;
        this.operationType = operationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    @Override
    public String toString() {
        return "IORequest{" +
                "id='" + id + '\'' +
                ", targetDeviceId='" + targetDeviceId + '\'' +
                ", payloadSize=" + payloadSize +
                ", operationType=" + operationType +
                '}';
    }
}
