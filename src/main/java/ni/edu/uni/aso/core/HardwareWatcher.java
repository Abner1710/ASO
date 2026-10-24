package ni.edu.uni.aso.core;

import javafx.application.Platform;

import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de monitoreo en tiempo real de Hardware del Sistema Operativo.
 * Implementa la detección de eventos Plug & Play (conexión y desconexión en caliente de USBs, discos y periféricos externos).
 * Realiza escaneo dual:
 * 1. Ultrarrápido (0.001s) para almacenamiento y memorias USB mediante File.listRoots().
 * 2. Asíncrono en segundo plano para periféricos externos (mouse USB, teclado, cooler, bluetooth, etc.)
 *    mediante la API de dispositivos PnP de Windows con Base64 script execution.
 */
public class HardwareWatcher {

    public interface HardwareListener {
        void onDeviceConnected(IODevice device, boolean isHotplug);
        void onDeviceDisconnected(String deviceId, String deviceName);
    }

    private final HardwareListener listener;
    private final Set<String> knownDriveRoots = new HashSet<>();
    private final Map<String, String> knownPnpDevices = new ConcurrentHashMap<>();
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private final AtomicBoolean isPnpScanning = new AtomicBoolean(false);
    private final AtomicBoolean isInitialPnpScan = new AtomicBoolean(true);

    public HardwareWatcher(HardwareListener listener) {
        this.listener = listener;
        initializeBaseline();
    }

    /**
     * Registra el estado inicial del hardware conectado al encender la aplicación.
     */
    private void initializeBaseline() {
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                knownDriveRoots.add(root.getAbsolutePath());
            }
        }
    }

    /**
     * Ejecuta una verificación del estado del hardware físico.
     * Combina escaneo instantáneo de unidades de disco con escaneo asíncrono de periféricos USB y PnP.
     */
    public void scan() {
        // 1. Escaneo ultrarrápido de unidades de almacenamiento (Memorias USB / Discos)
        scanDriveRoots();

        // 2. Escaneo en segundo plano de periféricos USB y PnP universales
        if (isPnpScanning.compareAndSet(false, true)) {
            Thread pnpThread = new Thread(this::scanWindowsUsbDevices, "USB-PnP-Scanner");
            pnpThread.setDaemon(true);
            pnpThread.start();
        }
    }

    /**
     * Escanea unidades de almacenamiento mediante la API nativa de Java (latencia < 1ms).
     */
    private void scanDriveRoots() {
        File[] currentRoots = File.listRoots();
        Set<String> currentRootPaths = new HashSet<>();

        if (currentRoots != null) {
            for (File root : currentRoots) {
                String path = root.getAbsolutePath();
                currentRootPaths.add(path);

                if (!knownDriveRoots.contains(path)) {
                    knownDriveRoots.add(path);
                    IODevice newDev = createDeviceFromRoot(root);
                    if (listener != null) {
                        Platform.runLater(() -> listener.onDeviceConnected(newDev, true));
                    }
                }
            }
        }

        Set<String> disconnected = new HashSet<>(knownDriveRoots);
        disconnected.removeAll(currentRootPaths);

        for (String disconnectedPath : disconnected) {
            knownDriveRoots.remove(disconnectedPath);
            String devId = "dev-" + disconnectedPath.replace(":", "").replace("\\", "").toLowerCase();
            if (listener != null) {
                Platform.runLater(() -> listener.onDeviceDisconnected(devId, "Unidad " + disconnectedPath));
            }
        }
    }

    /**
     * Escanea dispositivos conectados en Windows mediante PowerShell y PnP con codificación Base64.
     */
    private void scanWindowsUsbDevices() {
        try {
            // Script de PowerShell optimizado sin problemas de comillas o variables
            String psScript = "$ProgressPreference = 'SilentlyContinue'; " +
                    "Get-PnpDevice -PresentOnly | Where-Object { " +
                    "  ((($_.Class -in @('Mouse','Keyboard','Bluetooth','USB','DiskDrive','Ports')) -or ($_.InstanceId -like 'USB*')) -and " +
                    "   ($_.FriendlyName -and $_.FriendlyName -notmatch 'Root|Concentrador|Composite|Compuesto|Host controller|controladora host|eXtensible|Generador|Enumerador|Servicio|Software|Audio|TDI|AVRCP|proveedor|consumidor|GPIO|Filter|I2C|Controlador del sistema|Configuration Device|banda lateral|convertido')) " +
                    "} | ForEach-Object { " +
                    "  $_.InstanceId + ':::' + $_.FriendlyName + ':::' + $_.Class " +
                    "}";

            byte[] scriptBytes = psScript.getBytes(StandardCharsets.UTF_16LE);
            String encodedCommand = Base64.getEncoder().encodeToString(scriptBytes);

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encodedCommand);
            Process process = pb.start();

            Set<String> currentPnpIds = new HashSet<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || !line.contains(":::")) {
                        continue;
                    }

                    String[] parts = line.split(":::");
                    if (parts.length >= 2) {
                        String instanceId = parts[0].trim();
                        String friendlyName = parts[1].trim();
                        String devClass = parts.length > 2 ? parts[2].trim() : "USB";

                        currentPnpIds.add(instanceId);

                        // Si es un dispositivo nuevo que antes no estaba registrado en memoria
                        if (!knownPnpDevices.containsKey(instanceId)) {
                            // Evitar tarjetas duplicadas si una sola entidad física expone múltiples interfaces HID idénticas
                            boolean isDuplicate = false;
                            for (String existingName : knownPnpDevices.values()) {
                                if (existingName.equalsIgnoreCase(friendlyName)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            knownPnpDevices.put(instanceId, friendlyName);

                            if (!isDuplicate) {
                                String devId = "dev-pnp-" + Math.abs(friendlyName.hashCode() % 10000);
                                String assignedType = "USB";
                                String lowerName = friendlyName.toLowerCase();
                                if (devClass.equalsIgnoreCase("Keyboard") || lowerName.contains("teclado") || lowerName.contains("keyboard")) {
                                    assignedType = "Teclado";
                                } else if (devClass.equalsIgnoreCase("Mouse") || lowerName.contains("mouse") || lowerName.contains("ratón") || lowerName.contains("touchpad")) {
                                    assignedType = "Mouse";
                                } else if (devClass.equalsIgnoreCase("Bluetooth") || lowerName.contains("bluetooth")) {
                                    assignedType = "Bluetooth";
                                } else if (devClass.equalsIgnoreCase("DiskDrive") || lowerName.contains("disk") || lowerName.contains("disco")) {
                                    assignedType = "Disco";
                                }

                                IODevice pnpDev = new IODevice(devId, friendlyName, assignedType, 120, true, null, true);
                                pnpDev.setExtraInfo("Clase: " + devClass + " • Bus PnP en línea");

                                boolean isHotplug = !isInitialPnpScan.get();
                                if (listener != null) {
                                    Platform.runLater(() -> listener.onDeviceConnected(pnpDev, isHotplug));
                                }
                            }
                        }
                    }
                }
            }

            process.waitFor();
            isInitialPnpScan.set(false);

            // Detectar desconexión de dispositivos PnP retirados del sistema
            Set<String> disconnectedPnp = new HashSet<>(knownPnpDevices.keySet());
            disconnectedPnp.removeAll(currentPnpIds);

            for (String disId : disconnectedPnp) {
                String name = knownPnpDevices.remove(disId);
                // Si ya no queda ninguna interfaz asociada a este dispositivo, notificar desconexión
                if (name != null && !knownPnpDevices.containsValue(name)) {
                    String devId = "dev-pnp-" + Math.abs(name.hashCode() % 10000);
                    if (listener != null) {
                        Platform.runLater(() -> listener.onDeviceDisconnected(devId, name));
                    }
                }
            }

        } catch (Exception ignored) {
        } finally {
            isPnpScanning.set(false);
        }
    }

    /**
     * Construye un modelo IODevice a partir de una unidad de almacenamiento física detectada.
     */
    public IODevice createDeviceFromRoot(File root) {
        String path = root.getAbsolutePath();
        String letter = path.replace("\\", "");
        String devId = "dev-" + letter.replace(":", "").toLowerCase();

        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        double totalGb = totalSpace / (1024.0 * 1024.0 * 1024.0);
        double freeGb = freeSpace / (1024.0 * 1024.0 * 1024.0);

        String typeDesc;
        boolean isRemovable;
        try {
            typeDesc = fileSystemView.getSystemTypeDescription(root);
            if (typeDesc == null) typeDesc = "Disco";
            isRemovable = fileSystemView.isFloppyDrive(root) ||
                    typeDesc.toLowerCase().contains("extra") ||
                    typeDesc.toLowerCase().contains("remov") ||
                    typeDesc.toLowerCase().contains("usb") ||
                    !letter.equalsIgnoreCase("C:");
        } catch (Exception e) {
            typeDesc = "Almacenamiento";
            isRemovable = !letter.equalsIgnoreCase("C:");
        }

        String displayName = isRemovable ? "Memoria USB (" + letter + ")" : "Disco Local (" + letter + ")";
        IODevice device = new IODevice(devId, displayName, "Disco", 200, true, path, isRemovable);
        device.setTotalSpaceBytes(totalSpace);
        device.setFreeSpaceBytes(freeSpace);
        device.setExtraInfo(String.format("%.1f GB libres de %.1f GB (%s)", freeGb, totalGb, typeDesc));

        return device;
    }
}
