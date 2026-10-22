package ni.edu.uni.aso.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Administrador central del subsistema de Entrada/Salida.
 * Gestiona la cola de peticiones, el ciclo de vida operativo de los dispositivos y el registro de eventos del sistema.
 */
public class IOSubsystem {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Queue<IORequest> requestQueue;
    private final List<IODevice> devices;
    private final Queue<String> systemLogs = new LinkedList<>();

    /**
     * Inicializa el subsistema de E/S con la cola vacía y los 4 dispositivos predeterminados.
     */
    public IOSubsystem() {
        this.requestQueue = new LinkedList<>();
        this.devices = new ArrayList<>();

        // Inicialización de las 4 instancias especificadas
        this.devices.add(new IODevice("dev-1", "Disco", "Disco", 100));
        this.devices.add(new IODevice("dev-2", "Teclado", "Teclado", 50));
        this.devices.add(new IODevice("dev-3", "Impresora", "Impresora", 150));
        this.devices.add(new IODevice("dev-4", "Monitor Principal", "Monitor", 100));
    }

    /**
     * Registra un evento en el sistema con marca de tiempo actual (HH:mm:ss).
     *
     * @param message Mensaje a registrar.
     */
    private void log(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        systemLogs.offer(String.format("[%s] %s", timestamp, message));
    }

    /**
     * Extrae todos los mensajes de eventos acumulados, vacía la cola y los retorna en una lista.
     *
     * @return Lista de logs registrados hasta el momento.
     */
    public List<String> getAndClearLogs() {
        List<String> logs = new ArrayList<>();
        while (!systemLogs.isEmpty()) {
            logs.add(systemLogs.poll());
        }
        return logs;
    }

    /**
     * Agrega una petición de E/S a la cola de espera y registra el evento.
     *
     * @param request Petición de E/S a encolar.
     */
    public void addRequest(IORequest request) {
        if (request != null) {
            this.requestQueue.offer(request);
            log(String.format("Petición %s [%s] encolada.", request.getId(), request.getOperationType()));
        }
    }

    /**
     * Ejecuta un ciclo de reloj (tick) del subsistema:
     * 1. Despacha la siguiente petición si el dispositivo destino está LIBRE.
     * 2. Itera sobre los dispositivos:
     *    - Si está INTERRUMPIDO, la rutina ISR concluyó y se invoca resetInterrupt().
     *    - Si está OCUPADO, incrementa el buffer. Si alcanza o supera el
     *      payloadSize de la petición en curso, cambia su estado a INTERRUMPIDO.
     */
    public void tick() {
        // 1. Despacho de la cola si el dispositivo destino está LIBRE
        if (!requestQueue.isEmpty()) {
            IORequest nextRequest = requestQueue.peek();
            IODevice targetDevice = findDeviceById(nextRequest.getTargetDeviceId());

            if (targetDevice != null && targetDevice.getStatus() == DeviceStatus.LIBRE) {
                requestQueue.poll();
                targetDevice.setCurrentRequest(nextRequest);
                targetDevice.setStatus(DeviceStatus.OCUPADO);
                log("[Planificador] Petición asignada a " + targetDevice.getName());
            }
        }

        // 2. Actualización del ciclo para cada dispositivo
        for (IODevice device : devices) {
            if (device.getStatus() == DeviceStatus.INTERRUMPIDO) {
                // Finalización de rutina de servicio de interrupción (ISR)
                device.resetInterrupt();
                log("[Kernel] Dispositivo " + device.getName() + " liberado.");
            } else if (device.getStatus() == DeviceStatus.OCUPADO) {
                int newBuffer = device.getCurrentBuffer() + 8;
                device.setCurrentBuffer(newBuffer);

                IORequest req = device.getCurrentRequest();
                if (req != null && newBuffer >= req.getPayloadSize()) {
                    device.setStatus(DeviceStatus.INTERRUMPIDO);
                    log("[Hardware] Interrupción (ISR) generada por " + device.getName());
                }
            }
        }
    }

    /**
     * Busca un dispositivo por su identificador.
     *
     * @param deviceId ID del dispositivo a buscar.
     * @return El dispositivo correspondiente o null si no existe.
     */
    public IODevice findDeviceById(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        for (IODevice device : devices) {
            if (deviceId.equals(device.getId())) {
                return device;
            }
        }
        return null;
    }

    public Queue<IORequest> getRequestQueue() {
        return requestQueue;
    }

    public List<IODevice> getDevices() {
        return devices;
    }
}
