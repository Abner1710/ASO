package ni.edu.uni.aso.core;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Administrador central del subsistema de Entrada/Salida.
 * Gestiona la cola de peticiones, la interacción con hardware físico (PnP, almacenamiento real, interrupciones x86)
 * y el ciclo de vida operativo de los dispositivos.
 */
public class IOSubsystem {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Queue<IORequest> requestQueue;
    private final List<IODevice> devices;
    private final Queue<String> systemLogs = new LinkedList<>();

    /**
     * Inicializa el subsistema de E/S vinculando los dispositivos físicos reales de la computadora.
     */
    public IOSubsystem() {
        this.requestQueue = new LinkedList<>();
        // CopyOnWriteArrayList para concurrencia segura ante eventos Plug & Play dinámicos
        this.devices = new CopyOnWriteArrayList<>();

        // 1. Disco Local Físico (C:) - Siempre presente
        File rootC = new File("C:\\");
        long totalC = rootC.getTotalSpace();
        long freeC = rootC.getFreeSpace();
        double totalGb = totalC / (1024.0 * 1024.0 * 1024.0);
        double freeGb = freeC / (1024.0 * 1024.0 * 1024.0);

        IODevice diskC = new IODevice("dev-1", "Disco Local (C:)", "Disco", 100, true, "C:\\", false);
        diskC.setTotalSpaceBytes(totalC);
        diskC.setFreeSpaceBytes(freeC);
        diskC.setExtraInfo(String.format("%.1f GB libres de %.1f GB (NVMe/NTFS)", freeGb, totalGb));
        this.devices.add(diskC);

        // 2. Teclado Físico (IRQ 1 del estándar PC - Dispositivo de Entrada por Caracteres)
        IODevice keyboard = new IODevice("dev-2", "Teclado Físico (IRQ 1)", "Teclado", 50, true, null, false);
        keyboard.setExtraInfo("Buffer stdin vacío • Presiona cualquier tecla para capturar");
        this.devices.add(keyboard);

        // 3. Mouse / Ratón Físico (IRQ 12 - Dispositivo de Entrada por Eventos)
        IODevice mouse = new IODevice("dev-3", "Mouse / Puntero (IRQ 12)", "Mouse", 50, true, null, false);
        mouse.setExtraInfo("Coordenadas en espera • Haz click en la ventana para capturar");
        this.devices.add(mouse);

        // 4. Puerto USB Host Universal (Reemplaza al monitor para permitir probar cualquier periférico USB)
        IODevice usbHost = new IODevice("dev-4", "Puerto USB Host (PnP)", "USB", 100, true, null, false);
        usbHost.setExtraInfo("Listo para Plug & Play • Conecta una USB, celular o periférico");
        this.devices.add(usbHost);

        // 5. Escaneo inicial de otros discos/USBs ya conectados al arrancar
        detectInitialDrives();

        log("[Kernel PnP] Subsistema iniciado con hardware físico de la máquina anfitriona.");
    }

    private void detectInitialDrives() {
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                String path = root.getAbsolutePath();
                if (!path.equalsIgnoreCase("C:\\")) {
                    String letter = path.replace("\\", "");
                    String devId = "dev-" + letter.replace(":", "").toLowerCase();
                    long total = root.getTotalSpace();
                    long free = root.getFreeSpace();
                    double freeGb = free / (1024.0 * 1024.0 * 1024.0);

                    IODevice usbDev = new IODevice(devId, "Memoria USB (" + letter + ")", "Disco", 120, true, path, true);
                    usbDev.setTotalSpaceBytes(total);
                    usbDev.setFreeSpaceBytes(free);
                    usbDev.setExtraInfo(String.format("%.1f GB libres (USB Extraíble)", freeGb));
                    this.devices.add(usbDev);
                    log("[Plug & Play] Dispositivo de almacenamiento detectado al arranque: " + usbDev.getName());
                }
            }
        }
    }

    /**
     * Registra un evento en el sistema con marca de tiempo actual (HH:mm:ss).
     */
    public synchronized void log(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        systemLogs.offer(String.format("[%s] %s", timestamp, message));
    }

    /**
     * Extrae todos los mensajes de eventos acumulados, vacía la cola y los retorna en una lista.
     */
    public synchronized List<String> getAndClearLogs() {
        List<String> logs = new ArrayList<>();
        while (!systemLogs.isEmpty()) {
            logs.add(systemLogs.poll());
        }
        return logs;
    }

    /**
     * Incorpora un dispositivo detectado dinámicamente en caliente (Plug & Play).
     */
    public void addDynamicDevice(IODevice device) {
        if (device != null && findDeviceById(device.getId()) == null) {
            this.devices.add(device);
            log(String.format("[Plug & Play] Nuevo hardware conectado: %s [Controlador cargado con éxito].",
                    device.getName()));
        }
    }

    /**
     * Remueve o desvincula un dispositivo desconectado en caliente.
     */
    public void removeDynamicDevice(String deviceId, String deviceName) {
        IODevice dev = findDeviceById(deviceId);
        if (dev != null) {
            this.devices.remove(dev);
            log(String.format("[Plug & Play] Hardware desconectado: %s. Recursos de E/S liberados.", deviceName));
        }
    }

    /**
     * Procesa una interrupción generada por el teclado físico del usuario (IRQ 1).
     */
    public void handleKeyboardInterrupt(String text, String code) {
        IODevice kbd = findDeviceById("dev-2");
        if (kbd != null) {
            kbd.recordKeyStroke(text, code);
            String keyDesc = (text != null && !text.isEmpty()) ? "'" + text + "'" : code;
            log(String.format("[Hardware IRQ 1] Interrupción de teclado físico: Tecla %s (%s) almacenada en buffer stdin.", keyDesc, code));
        }
    }

    /**
     * Procesa una interrupción generada por el mouse/puntero físico (IRQ 12).
     */
    public void handleMouseInterrupt(String button, double x, double y) {
        IODevice mouse = findDeviceById("dev-3");
        if (mouse != null) {
            mouse.recordMouseAction(button, (int) x, (int) y);
            log(String.format("[Hardware IRQ 12] Interrupción de ratón físico: Click %s en posición (%d, %d).",
                    button, (int) x, (int) y));
        }
    }

    /**
     * Agrega una petición de E/S a la cola de espera y registra el evento.
     */
    public synchronized void addRequest(IORequest request) {
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
     *    - Si está OCUPADO, incrementa el buffer. Si alcanza el payload, ejecuta la E/S contextualizada
     *      (fsync en disco, stdin en teclado, etc.), mide latencia y dispara la interrupción (ISR).
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
                log("[Planificador] Petición " + nextRequest.getId() + " asignada a " + targetDevice.getName());
            }
        }

        // 2. Actualización del ciclo para cada dispositivo
        for (IODevice device : devices) {
            if (device.getStatus() == DeviceStatus.INTERRUMPIDO) {
                // Finalización de rutina de servicio de interrupción (ISR)
                device.resetInterrupt();
                log("[Kernel] Dispositivo " + device.getName() + " liberado tras atención de ISR.");
            } else if (device.getStatus() == DeviceStatus.OCUPADO) {
                int newBuffer = device.getCurrentBuffer() + 16;
                device.setCurrentBuffer(newBuffer);

                IORequest req = device.getCurrentRequest();
                if (req != null && newBuffer >= req.getPayloadSize()) {
                    // Ejecución de la operación contextualizada del hardware
                    String ioResult = device.executePhysicalIO(req.getOperationType(), req.getPayloadSize());
                    log("[Hardware E/S] " + ioResult);

                    device.setStatus(DeviceStatus.INTERRUMPIDO);
                    log("[Hardware] Interrupción (ISR) generada por " + device.getName());
                }
            }
        }
    }

    /**
     * Busca un dispositivo por su identificador.
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
