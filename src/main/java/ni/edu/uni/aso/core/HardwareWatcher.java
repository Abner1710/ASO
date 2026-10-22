package ni.edu.uni.aso.core;

import javafx.application.Platform;
import javafx.stage.Screen;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Servicio de monitoreo en tiempo real de Hardware del Sistema Operativo.
 * Implementa la detección de eventos Plug & Play (conexión y desconexión en caliente de USBs, discos y pantallas).
 */
public class HardwareWatcher {

    public interface HardwareListener {
        void onDriveConnected(IODevice device);
        void onDriveDisconnected(String deviceId, String drivePath);
        void onScreenChanged(int totalScreens, String details);
    }

    private final HardwareListener listener;
    private final Set<String> knownDriveRoots = new HashSet<>();
    private int knownScreenCount = 0;
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();

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
        try {
            knownScreenCount = Screen.getScreens().size();
        } catch (Exception ignored) {
            knownScreenCount = 1;
        }
    }

    /**
     * Ejecuta una verificación del estado del hardware físico y dispara eventos ante cambios.
     */
    public void scan() {
        // 1. Escaneo de Unidades de Almacenamiento (USB / Discos PnP)
        File[] currentRoots = File.listRoots();
        Set<String> currentRootPaths = new HashSet<>();

        if (currentRoots != null) {
            for (File root : currentRoots) {
                String path = root.getAbsolutePath();
                currentRootPaths.add(path);

                // Nuevo periférico detectado (Plug & Play)
                if (!knownDriveRoots.contains(path)) {
                    knownDriveRoots.add(path);
                    IODevice newDev = createDeviceFromRoot(root);
                    if (listener != null) {
                        Platform.runLater(() -> listener.onDriveConnected(newDev));
                    }
                }
            }
        }

        // Detección de desconexión en caliente (Dispositivo retirado)
        Set<String> disconnected = new HashSet<>(knownDriveRoots);
        disconnected.removeAll(currentRootPaths);

        for (String disconnectedPath : disconnected) {
            knownDriveRoots.remove(disconnectedPath);
            String devId = "dev-" + disconnectedPath.replace(":", "").replace("\\", "").toLowerCase();
            if (listener != null) {
                Platform.runLater(() -> listener.onDriveDisconnected(devId, disconnectedPath));
            }
        }

        // 2. Escaneo de Monitores / Pantallas
        try {
            int currentScreens = Screen.getScreens().size();
            if (currentScreens != knownScreenCount) {
                knownScreenCount = currentScreens;
                String details = currentScreens > 1
                        ? currentScreens + " Pantallas conectadas (Modo Extendido/HDMI)"
                        : "1 Pantalla principal activa";
                if (listener != null) {
                    Platform.runLater(() -> listener.onScreenChanged(currentScreens, details));
                }
            }
        } catch (Exception ignored) {
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
        boolean isRemovable = false;
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
