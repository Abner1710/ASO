package ni.edu.uni.aso.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Representa un dispositivo de Entrada/Salida gestionado por el simulador.
 * Soporta tanto controladores virtuales como dispositivos físicos reales (Discos NVMe/SATA, USB PnP, Pantallas, Teclados).
 */
public class IODevice {
    private String id;
    private String name;
    private String type; // "Disco", "Teclado", "Mouse", "Monitor", "Impresora"
    private DeviceStatus status = DeviceStatus.LIBRE;
    private int bufferSize;
    private int currentBuffer = 0;
    private IORequest currentRequest;

    // Atributos de integración con Hardware Físico Real del Sistema Operativo
    private boolean isPhysical = false;
    private boolean isRemovable = false;
    private String physicalPath; // Ej: "C:\\", "E:\\"
    private long totalSpaceBytes = 0;
    private long freeSpaceBytes = 0;
    private double lastLatencyMs = 0.0;
    private double lastThroughputMBs = 0.0;
    private String extraInfo = "";

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

    public IODevice(String id, String name, String type, int bufferSize, boolean isPhysical, String physicalPath, boolean isRemovable) {
        this(id, name, type, bufferSize);
        this.isPhysical = isPhysical;
        this.physicalPath = physicalPath;
        this.isRemovable = isRemovable;
    }

    /**
     * Limpia el buffer actual, desvincula la petición en curso y restablece el estado a LIBRE.
     */
    public void resetInterrupt() {
        this.currentBuffer = 0;
        this.currentRequest = null;
        this.status = DeviceStatus.LIBRE;
    }

    /**
     * Ejecuta una operación de E/S física real sobre el medio de almacenamiento usando llamadas del SO.
     * Utiliza la primitiva de sincronización fsync() para medir la latencia física real de la controladora.
     *
     * @param op           Tipo de operación (LECTURA o ESCRITURA).
     * @param payloadUnits Unidades de carga útil (se mapean a bloques físicos de 64 KB).
     * @return Mensaje descriptivo con métricas reales de hardware.
     */
    public String executePhysicalIO(OperationType op, int payloadUnits) {
        if (!isPhysical || physicalPath == null) {
            return "Operación simulada en memoria.";
        }

        File targetDir = new File(physicalPath);
        if (!targetDir.exists() || !targetDir.canWrite()) {
            targetDir = new File(System.getProperty("java.io.tmpdir"));
        }

        File testFile = new File(targetDir, ".aso_io_benchmark.tmp");
        int bytesToWrite = Math.max(1024, payloadUnits * 1024 * 64); // Mínimo 64 KB por ráfaga
        long startTime = System.nanoTime();

        try {
            if (op == OperationType.ESCRITURA) {
                byte[] buffer = new byte[bytesToWrite];
                new Random().nextBytes(buffer);
                try (FileOutputStream fos = new FileOutputStream(testFile)) {
                    fos.write(buffer);
                    fos.flush();
                    // Invocación a la primitiva fsync() del kernel del Sistema Operativo
                    fos.getFD().sync();
                }
            } else {
                // Operación de LECTURA
                if (!testFile.exists()) {
                    // Si no existe, crear un archivo base de prueba
                    try (FileOutputStream fos = new FileOutputStream(testFile)) {
                        fos.write(new byte[bytesToWrite]);
                        fos.getFD().sync();
                    }
                }
                byte[] readBuffer = new byte[4096];
                int totalRead = 0;
                try (FileInputStream fis = new FileInputStream(testFile)) {
                    int bytesRead;
                    while ((bytesRead = fis.read(readBuffer)) != -1 && totalRead < bytesToWrite) {
                        totalRead += bytesRead;
                    }
                }
            }

            long elapsedNanos = System.nanoTime() - startTime;
            this.lastLatencyMs = Math.max(0.01, elapsedNanos / 1_000_000.0);
            double seconds = this.lastLatencyMs / 1000.0;
            double megabytes = bytesToWrite / (1024.0 * 1024.0);
            this.lastThroughputMBs = seconds > 0 ? (megabytes / seconds) : 0.0;

            return String.format("%s real de %.2f MB en %.2f ms (%.1f MB/s) en %s",
                    op == OperationType.ESCRITURA ? "Escritura física" : "Lectura física",
                    megabytes, this.lastLatencyMs, this.lastThroughputMBs, testFile.getAbsolutePath());

        } catch (IOException e) {
            return "Error de E/S en hardware: " + e.getMessage();
        }
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

    public boolean isPhysical() {
        return isPhysical;
    }

    public void setPhysical(boolean physical) {
        isPhysical = physical;
    }

    public boolean isRemovable() {
        return isRemovable;
    }

    public void setRemovable(boolean removable) {
        isRemovable = removable;
    }

    public String getPhysicalPath() {
        return physicalPath;
    }

    public void setPhysicalPath(String physicalPath) {
        this.physicalPath = physicalPath;
    }

    public long getTotalSpaceBytes() {
        return totalSpaceBytes;
    }

    public void setTotalSpaceBytes(long totalSpaceBytes) {
        this.totalSpaceBytes = totalSpaceBytes;
    }

    public long getFreeSpaceBytes() {
        return freeSpaceBytes;
    }

    public void setFreeSpaceBytes(long freeSpaceBytes) {
        this.freeSpaceBytes = freeSpaceBytes;
    }

    public double getLastLatencyMs() {
        return lastLatencyMs;
    }

    public void setLastLatencyMs(double lastLatencyMs) {
        this.lastLatencyMs = lastLatencyMs;
    }

    public double getLastThroughputMBs() {
        return lastThroughputMBs;
    }

    public void setLastThroughputMBs(double lastThroughputMBs) {
        this.lastThroughputMBs = lastThroughputMBs;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public String toString() {
        return "IODevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isPhysical=" + isPhysical +
                ", status=" + status +
                ", bufferSize=" + bufferSize +
                ", currentBuffer=" + currentBuffer +
                ", currentRequest=" + (currentRequest != null ? currentRequest.getId() : "null") +
                '}';
    }
}
