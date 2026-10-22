package ni.edu.uni.aso.core;

/**
 * Representa un dispositivo de Entrada/Salida gestionado por el simulador.
 */
public class IODevice {
    private String id;
    private String name;
    private String type; // "Monitor", "Disco", "Teclado", "Impresora"
    private DeviceStatus status = DeviceStatus.LIBRE;
    private int bufferSize;
    private int currentBuffer = 0;
    private IORequest currentRequest;

    public IODevice() {
        this.status = DeviceStatus.LIBRE;
        this.currentBuffer = 0;
        this.currentRequest = null;
    }

    public IODevice(String id, String name, String type, int bufferSize) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bufferSize = bufferSize;
        this.status = DeviceStatus.LIBRE;
        this.currentBuffer = 0;
        this.currentRequest = null;
    }

    /**
     * Limpia el buffer actual, desvincula la petición en curso y restablece el estado a LIBRE.
     */
    public void resetInterrupt() {
        this.currentBuffer = 0;
        this.currentRequest = null;
        this.status = DeviceStatus.LIBRE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getCurrentBuffer() {
        return currentBuffer;
    }

    public void setCurrentBuffer(int currentBuffer) {
        this.currentBuffer = currentBuffer;
    }

    public IORequest getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(IORequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    @Override
    public String toString() {
        return "IODevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", status=" + status +
                ", bufferSize=" + bufferSize +
                ", currentBuffer=" + currentBuffer +
                ", currentRequest=" + (currentRequest != null ? currentRequest.getId() : "null") +
                '}';
    }
}
