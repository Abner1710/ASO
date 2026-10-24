package ni.edu.uni.aso.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import ni.edu.uni.aso.core.DeviceStatus;
import ni.edu.uni.aso.core.HardwareWatcher;
import ni.edu.uni.aso.core.IODevice;
import ni.edu.uni.aso.core.IORequest;
import ni.edu.uni.aso.core.IOSubsystem;
import ni.edu.uni.aso.core.OperationType;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Controlador principal del Dashboard de Simulación de Entrada/Salida.
 * Integra:
 * - Operaciones de E/S contextualizadas para Teclado (stdin/ioctl), Mouse (deltas/calibración), Disco (fsync) y USB.
 * - Animaciones llamativas de detección Plug & Play en caliente (Toast Notification con audio chime y rebote elástico).
 * - Escaneo de cualquier dispositivo USB externo conectado (Mouse, Teclado, Cooler con chip, Celular, Memoria).
 */
public class DashboardController {

    // Contenedor principal
    @FXML private VBox mainContainer;

    // Banner Toast Flotante de Detección
    @FXML private HBox detectionToast;
    @FXML private HBox toastIconBox;
    @FXML private Region toastPulseDot;
    @FXML private Label lblToastTitle;
    @FXML private Label lblToastBadge;
    @FXML private Label lblToastDevice;
    @FXML private Label lblToastDesc;
    @FXML private ProgressBar toastProgressBar;
    @FXML private Button btnDismissToast;

    // Controles FXML - Botones y Formularios
    @FXML private Button btnReadRequest;
    @FXML private Button btnWriteRequest;
    @FXML private ToggleButton btnStressTest;
    @FXML private Button btnClearLogs;
    @FXML private Button btnRescanHardware;
    @FXML private Button btnSimulatePnp;
    @FXML private ComboBox<String> cmbTargetDevice;
    @FXML private Label lblDeviceContextDesc;

    // Controles FXML - KPIs Superiores
    @FXML private Label lblTotalRequests;
    @FXML private Label lblQueueCount;
    @FXML private Label lblActiveDevices;
    @FXML private Label lblStressStatus;
    @FXML private VBox boxKpi1;
    @FXML private VBox boxKpi2;
    @FXML private VBox boxKpi3;
    @FXML private VBox boxKpi4;

    // Controles FXML - Vistas de Datos y Hardware
    @FXML private ListView<IORequest> queueListView;
    @FXML private VBox devicesContainer;
    @FXML private TextArea logConsole;

    // Estado del Sistema
    private final IOSubsystem subsystem = new IOSubsystem();
    private final Random random = new Random();
    private int requestCounter = 1;
    private int totalRequestsGenerated = 0;
    private int demoDeviceIndex = 0;
    private Timeline stressTimeline;
    private Timeline toastCountdownTimeline;
    private HardwareWatcher hardwareWatcher;
    private final Map<String, DeviceStatus> lastKnownDeviceStatus = new HashMap<>();
    private final java.util.Set<String> newlyActivatedDeviceIds = new java.util.HashSet<>();

    @FXML
    public void initialize() {
        setupIcons();
        setupQueueListView();
        setupHardwareWatcher();
        updateTargetDeviceCombo();
        setupTargetDeviceListener();

        if (btnDismissToast != null) {
            btnDismissToast.setOnAction(e -> hideDetectionToast());
        }

        // Enlazar eventos de botones manuales
        if (btnReadRequest != null) {
            btnReadRequest.setOnAction(e -> handleGenerateRequest(OperationType.LECTURA));
        }
        if (btnWriteRequest != null) {
            btnWriteRequest.setOnAction(e -> handleGenerateRequest(OperationType.ESCRITURA));
        }
        if (btnClearLogs != null) {
            btnClearLogs.setOnAction(e -> {
                if (logConsole != null) {
                    logConsole.clear();
                    logConsole.appendText("[Sistema] Consola de telemetría reiniciada por el operador.\n");
                }
            });
        }
        if (btnRescanHardware != null) {
            btnRescanHardware.setOnAction(e -> {
                if (hardwareWatcher != null) {
                    hardwareWatcher.scan();
                    subsystem.log("[Manual PnP] Escaneo forzado del bus de hardware USB ejecutado.");
                    updateTargetDeviceCombo();
                    updateUI();
                }
            });
        }
        if (btnSimulatePnp != null) {
            btnSimulatePnp.setOnAction(e -> handleDemoPnpConnection());
        }

        // Listener de teclas físicas y clicks en la escena para interrupciones en tiempo real (IRQ 1 & 12)
        if (mainContainer != null) {
            mainContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setOnKeyPressed(event -> {
                        IODevice kbd = subsystem.findDeviceById("dev-2");
                        boolean wasOff = (kbd != null && kbd.getStatus() == DeviceStatus.APAGADO);
                        subsystem.handleKeyboardInterrupt(event.getText(), event.getCode().toString());
                        if (wasOff && kbd != null) {
                            newlyActivatedDeviceIds.add("dev-2");
                            showDetectionToast("¡TECLADO FÍSICO ACTIVADO!", "Teclado Físico (IRQ 1)",
                                    "Pulsación de tecla detectada • Asignado a vector de interrupción IRQ 1", true);
                        }
                        updateUI();
                    });
                    newScene.setOnMouseClicked(event -> {
                        IODevice mouse = subsystem.findDeviceById("dev-3");
                        boolean wasOff = (mouse != null && mouse.getStatus() == DeviceStatus.APAGADO);
                        subsystem.handleMouseInterrupt(event.getButton().toString(), event.getX(), event.getY());
                        if (wasOff && mouse != null) {
                            newlyActivatedDeviceIds.add("dev-3");
                            showDetectionToast("¡PUNTERO FÍSICO ACTIVADO!", "Mouse / Puntero (IRQ 12)",
                                    "Interacción de ratón detectada • Asignado a vector de interrupción IRQ 12", true);
                        }
                        updateUI();
                    });
                }
            });
        }

        // Configuración de Stress Timeline (cada 2200 ms)
        stressTimeline = new Timeline(new KeyFrame(Duration.millis(2200), event -> {
            OperationType randomOp = Math.random() < 0.5 ? OperationType.LECTURA : OperationType.ESCRITURA;
            handleGenerateRequest(randomOp);
        }));
        stressTimeline.setCycleCount(Timeline.INDEFINITE);

        if (btnStressTest != null) {
            btnStressTest.selectedProperty().addListener((obs, oldVal, isSelected) -> {
                if (isSelected) {
                    stressTimeline.play();
                    if (lblStressStatus != null) {
                        lblStressStatus.setText("ACTIVO (TURBO)");
                        lblStressStatus.getStyleClass().removeAll("kpi-status-off");
                        lblStressStatus.getStyleClass().add("kpi-status-on");
                    }
                    if (logConsole != null) {
                        logConsole.appendText("[Stress Test] Generador automático de carga iniciado.\n");
                    }
                } else {
                    stressTimeline.stop();
                    if (lblStressStatus != null) {
                        lblStressStatus.setText("INACTIVO");
                        lblStressStatus.getStyleClass().removeAll("kpi-status-on");
                        lblStressStatus.getStyleClass().add("kpi-status-off");
                    }
                    if (logConsole != null) {
                        logConsole.appendText("[Stress Test] Generador automático detenido.\n");
                    }
                }
            });
        }

        // Renderizado inicial
        updateUI();

        // Reloj maestro del Kernel (cada 1500 ms)
        Timeline kernelClock = new Timeline(new KeyFrame(Duration.millis(1500), event -> {
            subsystem.tick();
            updateUI();
        }));
        kernelClock.setCycleCount(Timeline.INDEFINITE);
        kernelClock.play();
    }

    /**
     * Muestra la animación visual y sonora de detección de hardware (Toast Banner).
     * Integra resorte elástico (scale bounce), barra animada de cuenta regresiva y cierre rápido.
     */
    private void showDetectionToast(String title, String deviceName, String desc, boolean isConnected) {
        if (detectionToast == null) return;

        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}

        if (toastCountdownTimeline != null) {
            toastCountdownTimeline.stop();
        }

        lblToastTitle.setText(title);
        lblToastDevice.setText(deviceName);
        lblToastDesc.setText(desc);

        detectionToast.getStyleClass().removeAll("toast-connected", "toast-disconnected");
        detectionToast.getStyleClass().add(isConnected ? "toast-connected" : "toast-disconnected");

        if (toastPulseDot != null) {
            toastPulseDot.getStyleClass().removeAll("dot-connected", "dot-disconnected");
            toastPulseDot.getStyleClass().add(isConnected ? "dot-connected" : "dot-disconnected");
        }

        if (lblToastBadge != null) {
            lblToastBadge.setText(isConnected ? "PnP ONLINE" : "PnP OFFLINE");
            lblToastBadge.setStyle(isConnected
                    ? "-fx-text-fill: #34d399; -fx-background-color: rgba(16, 185, 129, 0.2); -fx-padding: 2px 7px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #f87171; -fx-background-color: rgba(239, 68, 68, 0.2); -fx-padding: 2px 7px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
        }

        if (toastIconBox != null) {
            String iconPath = isConnected ? IconHelper.ICON_USB : IconHelper.ICON_ALERT;
            String iconColor = isConnected ? "#10b981" : "#ef4444";
            toastIconBox.getChildren().setAll(IconHelper.createIcon(iconPath, iconColor, 1.25));
        }

        if (toastProgressBar != null) {
            toastProgressBar.setProgress(1.0);
            toastProgressBar.setStyle(isConnected ? "-fx-accent: #10b981;" : "-fx-accent: #ef4444;");
        }

        detectionToast.setVisible(true);
        detectionToast.setManaged(true);
        detectionToast.setOpacity(0.0);
        detectionToast.setTranslateY(-20);
        detectionToast.setScaleX(0.92);
        detectionToast.setScaleY(0.92);

        // Animación dinámica de entrada: Elastic Spring + Slide Down + Fade In
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), detectionToast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition slideDown = new TranslateTransition(Duration.millis(300), detectionToast);
        slideDown.setFromY(-20);
        slideDown.setToY(0);

        ScaleTransition scaleBounce = new ScaleTransition(Duration.millis(300), detectionToast);
        scaleBounce.setFromX(0.92);
        scaleBounce.setFromY(0.92);
        scaleBounce.setToX(1.03);
        scaleBounce.setToY(1.03);
        scaleBounce.setOnFinished(e -> {
            ScaleTransition settle = new ScaleTransition(Duration.millis(120), detectionToast);
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.play();
        });

        fadeIn.play();
        slideDown.play();
        scaleBounce.play();

        // Barra de progreso con cuenta regresiva dinámica (4.0 segundos)
        int totalSteps = 40;
        final double[] currentProg = {1.0};
        toastCountdownTimeline = new Timeline(new KeyFrame(Duration.millis(100), ev -> {
            currentProg[0] -= (1.0 / totalSteps);
            if (toastProgressBar != null) {
                toastProgressBar.setProgress(Math.max(0, currentProg[0]));
            }
        }));
        toastCountdownTimeline.setCycleCount(totalSteps);
        toastCountdownTimeline.setOnFinished(ev -> hideDetectionToast());
        toastCountdownTimeline.play();
    }

    /**
     * Oculta el Toast de detección con animación suave de salida.
     */
    private void hideDetectionToast() {
        if (detectionToast == null || !detectionToast.isVisible()) return;
        if (toastCountdownTimeline != null) {
            toastCountdownTimeline.stop();
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), detectionToast);
        fadeOut.setFromValue(detectionToast.getOpacity());
        fadeOut.setToValue(0.0);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(300), detectionToast);
        slideUp.setToY(-20);

        fadeOut.setOnFinished(e -> {
            detectionToast.setVisible(false);
            detectionToast.setManaged(false);
        });

        fadeOut.play();
        slideUp.play();
    }

    /**
     * Configura el listener del ComboBox de destino para contextualizar los botones de lectura y escritura.
     */
    private void setupTargetDeviceListener() {
        if (cmbTargetDevice == null) return;

        cmbTargetDevice.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String lower = newVal.toLowerCase();

            if (lower.contains("teclado")) {
                if (lblDeviceContextDesc != null) {
                    lblDeviceContextDesc.setText("Teclado: Lectura de pulsaciones (stdin/sys_read) y comandos ioctl para LEDs.");
                }
                if (btnReadRequest != null) btnReadRequest.setText("Leer Flujo Teclado (sys_read)");
                if (btnWriteRequest != null) btnWriteRequest.setText("Comando de Control (ioctl LED)");
            } else if (lower.contains("mouse") || lower.contains("ratón") || lower.contains("puntero")) {
                if (lblDeviceContextDesc != null) {
                    lblDeviceContextDesc.setText("Mouse: Lectura de deltas de movimiento/clicks (read /dev/input) y calibración.");
                }
                if (btnReadRequest != null) btnReadRequest.setText("Leer Paquete Movimiento (read)");
                if (btnWriteRequest != null) btnWriteRequest.setText("Calibrar Frecuencia (ioctl)");
            } else if (lower.contains("disco") || lower.contains("memoria") || lower.contains("c:")) {
                if (lblDeviceContextDesc != null) {
                    lblDeviceContextDesc.setText("Disco/USB: Lectura de sectores y volcado físico a celdas de memoria con fsync().");
                }
                if (btnReadRequest != null) btnReadRequest.setText("Leer Bloque de Disco (sys_read)");
                if (btnWriteRequest != null) btnWriteRequest.setText("Escribir Bloque Físico (fsync)");
            } else if (lower.contains("usb") || lower.contains("pnp")) {
                if (lblDeviceContextDesc != null) {
                    lblDeviceContextDesc.setText("USB: Transmisiones IN (descriptores de hardware) y OUT (tramas de control SETUP).");
                }
                if (btnReadRequest != null) btnReadRequest.setText("Leer Descriptor USB (Trans. IN)");
                if (btnWriteRequest != null) btnWriteRequest.setText("Enviar Paquete SETUP (Trans. OUT)");
            } else {
                if (lblDeviceContextDesc != null) {
                    lblDeviceContextDesc.setText("Automático: Selecciona cualquier dispositivo disponible en el bus del sistema.");
                }
                if (btnReadRequest != null) btnReadRequest.setText("Generar Petición (Lectura)");
                if (btnWriteRequest != null) btnWriteRequest.setText("Generar Petición (Escritura fsync)");
            }
        });
    }

    /**
     * Simula la conexión de un periférico de prueba para demostración en clase.
     */
    private void handleDemoPnpConnection() {
        String[] demoNames = {
                "Cooler Pad USB RGB (Hub Controller)",
                "Mouse Óptico USB Gaming (HID)",
                "Infinix Note 40 Pro (MTP USB)",
                "Memoria USB Kingston DataTraveler 3.0"
        };
        String name = demoNames[demoDeviceIndex % demoNames.length];
        demoDeviceIndex++;

        IODevice dev5 = subsystem.findDeviceById("dev-5");
        if (dev5 != null && dev5.getStatus() == DeviceStatus.APAGADO) {
            dev5.setStatus(DeviceStatus.LIBRE);
            dev5.setExtraInfo("● En línea • " + name + " conectado");
            newlyActivatedDeviceIds.add("dev-5");
            showDetectionToast("¡NUEVO HARDWARE DETECTADO EN CALIENTE!", name,
                    "Periférico USB/Cooler energizado y reconocido en bus PnP", true);
            subsystem.log("[Demo PnP] Periférico vinculado a dev-5: " + name);
        } else {
            String devId = "dev-demo-" + System.currentTimeMillis() % 1000;
            IODevice demoDev = new IODevice(devId, name, "USB", 120, true, null, true);
            demoDev.setExtraInfo("Conexión USB 3.0 • Tasa de transferencia: 480 Mbps");
            demoDev.setStatus(DeviceStatus.LIBRE);
            subsystem.addDynamicDevice(demoDev);
            newlyActivatedDeviceIds.add(devId);
            showDetectionToast("¡NUEVO HARDWARE DETECTADO EN CALIENTE!", name,
                    "Controlador USB asignado • Pipe de comunicación inicializado", true);
        }
        updateTargetDeviceCombo();
        updateUI();
    }

    /**
     * Inicializa el servicio de vigilancia de hardware Plug & Play.
     */
    private void setupHardwareWatcher() {
        hardwareWatcher = new HardwareWatcher(new HardwareWatcher.HardwareListener() {
            @Override
            public void onDeviceConnected(IODevice device, boolean isHotplug) {
                handleDeviceConnection(device, isHotplug);
            }

            @Override
            public void onDeviceDisconnected(String deviceId, String deviceName) {
                handleDeviceDisconnection(deviceId, deviceName);
            }
        });

        // Hilo de sondeo de hardware en caliente cada 2000 ms
        Timeline pnpWatcherTimeline = new Timeline(new KeyFrame(Duration.millis(2000), e -> {
            if (hardwareWatcher != null) {
                hardwareWatcher.scan();
            }
        }));
        pnpWatcherTimeline.setCycleCount(Timeline.INDEFINITE);
        pnpWatcherTimeline.play();
    }

    /**
     * Procesa la conexión de un dispositivo detectado vinculándolo inteligentemente a los controladores predeterminados
     * o agregándolo como tarjeta PnP dinámica.
     */
    private void handleDeviceConnection(IODevice device, boolean isHotplug) {
        String devName = device.getName();
        String devClass = device.getType();
        String lowerName = devName.toLowerCase();

        // 1. Vincular a Teclado Físico (dev-2) si está apagado
        IODevice kbd = subsystem.findDeviceById("dev-2");
        if (kbd != null && kbd.getStatus() == DeviceStatus.APAGADO &&
                (devClass.equalsIgnoreCase("Teclado") || lowerName.contains("teclado") || lowerName.contains("keyboard"))) {
            kbd.setStatus(DeviceStatus.LIBRE);
            kbd.setExtraInfo("● En línea • " + devName + " • IRQ 1");
            newlyActivatedDeviceIds.add("dev-2");
            showDetectionToast("¡CONTROLADOR DE TECLADO EN LÍNEA!", devName,
                    "Dispositivo físico detectado y vinculado a IRQ 1", true);
            subsystem.log("[Plug & Play] Teclado físico vinculado al subsistema: " + devName);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // 2. Vincular a Mouse/Puntero (dev-3) si está apagado
        IODevice mouse = subsystem.findDeviceById("dev-3");
        if (mouse != null && mouse.getStatus() == DeviceStatus.APAGADO &&
                (devClass.equalsIgnoreCase("Mouse") || lowerName.contains("mouse") || lowerName.contains("ratón") || lowerName.contains("touchpad"))) {
            mouse.setStatus(DeviceStatus.LIBRE);
            mouse.setExtraInfo("● En línea • " + devName + " • IRQ 12");
            newlyActivatedDeviceIds.add("dev-3");
            showDetectionToast("¡CONTROLADOR DE PUNTERO EN LÍNEA!", devName,
                    "Dispositivo físico detectado y vinculado a IRQ 12", true);
            subsystem.log("[Plug & Play] Mouse físico vinculado al subsistema: " + devName);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // 3. Vincular a Controlador Bluetooth (dev-4) si está apagado
        IODevice bluetooth = subsystem.findDeviceById("dev-4");
        if (bluetooth != null && bluetooth.getStatus() == DeviceStatus.APAGADO &&
                (devClass.equalsIgnoreCase("Bluetooth") || lowerName.contains("bluetooth"))) {
            bluetooth.setStatus(DeviceStatus.LIBRE);
            bluetooth.setExtraInfo("● En línea • " + devName + " • Radio PnP");
            newlyActivatedDeviceIds.add("dev-4");
            showDetectionToast("¡DRIVER BLUETOOTH EN LÍNEA!", devName,
                    "Adaptador de radio inalámbrico reconocido y activado", true);
            subsystem.log("[Plug & Play] Radio Bluetooth vinculada al subsistema: " + devName);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // 4. Vincular a Puerto USB Host / Cooler (dev-5) si está apagado
        IODevice usbHost = subsystem.findDeviceById("dev-5");
        if (usbHost != null && usbHost.getStatus() == DeviceStatus.APAGADO &&
                (lowerName.contains("cooler") || lowerName.contains("usb") || lowerName.contains("hub") || device.isRemovable())) {
            usbHost.setStatus(DeviceStatus.LIBRE);
            usbHost.setExtraInfo("● En línea • " + devName + " conectado");
            newlyActivatedDeviceIds.add("dev-5");
            showDetectionToast("¡NUEVO HARDWARE DETECTADO EN CALIENTE!", devName,
                    "Periférico USB/Cooler energizado y reconocido en bus PnP", true);
            subsystem.log("[Plug & Play] Dispositivo USB vinculado a puerto host: " + devName);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // 5. De lo contrario, registrar como nuevo dispositivo dinámico independiente
        device.setStatus(DeviceStatus.LIBRE);
        subsystem.addDynamicDevice(device);
        newlyActivatedDeviceIds.add(device.getId());
        showDetectionToast("¡NUEVO HARDWARE DETECTADO EN CALIENTE!", devName,
                "Controlador PnP cargado con éxito • Descriptores de hardware asignados", true);
        updateTargetDeviceCombo();
        updateUI();
    }

    /**
     * Procesa la desconexión de hardware en caliente retirándolo de la lista o regresando a standby.
     */
    private void handleDeviceDisconnection(String deviceId, String deviceName) {
        // Verificar si es un dispositivo dinámico
        IODevice dev = subsystem.findDeviceById(deviceId);
        if (dev != null) {
            subsystem.removeDynamicDevice(deviceId, deviceName);
            showDetectionToast("HARDWARE DESCONECTADO", deviceName,
                    "Periférico retirado en caliente • Liberando búferes y descriptores de E/S", false);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // Verificar si corresponde a dev-5 (USB Host/Cooler)
        IODevice usbHost = subsystem.findDeviceById("dev-5");
        if (usbHost != null && usbHost.getStatus() != DeviceStatus.APAGADO &&
                (usbHost.getExtraInfo().contains(deviceName) || deviceName.toLowerCase().contains("cooler") || deviceName.toLowerCase().contains("usb"))) {
            usbHost.setStatus(DeviceStatus.APAGADO);
            usbHost.setExtraInfo("○ Standby • Esperando conexión de Cooler, USB o periférico...");
            showDetectionToast("HARDWARE DESCONECTADO", deviceName,
                    "Periférico retirado en caliente • Puerto USB en standby", false);
            updateTargetDeviceCombo();
            updateUI();
            return;
        }

        // Aviso genérico de desconexión
        showDetectionToast("HARDWARE DESCONECTADO", deviceName,
                "Periférico retirado en caliente • Recursos liberados por el Kernel", false);
        updateTargetDeviceCombo();
        updateUI();
    }

    /**
     * Actualiza el listado desplegable de dispositivos objetivo para permitir pruebas dirigidas.
     */
    private void updateTargetDeviceCombo() {
        if (cmbTargetDevice == null) return;
        String currentSelection = cmbTargetDevice.getValue();

        cmbTargetDevice.getItems().clear();
        cmbTargetDevice.getItems().add("[Automático / Cualquier Dispositivo]");
        for (IODevice dev : subsystem.getDevices()) {
            String statusSuffix = dev.getStatus() == DeviceStatus.APAGADO ? " (STANDBY)" : " (ONLINE)";
            cmbTargetDevice.getItems().add(dev.getName() + " (" + dev.getId() + ")" + statusSuffix);
        }

        if (currentSelection != null && cmbTargetDevice.getItems().contains(currentSelection)) {
            cmbTargetDevice.setValue(currentSelection);
        } else {
            cmbTargetDevice.getSelectionModel().selectFirst();
        }
    }

    /**
     * Inyecta iconos vectoriales SVG en los botones, KPI cards y componentes visuales.
     */
    private void setupIcons() {
        if (btnReadRequest != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_READ, "#38bdf8", 1.05);
            btnReadRequest.setGraphic(icon);
        }
        if (btnWriteRequest != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_WRITE, "#c084fc", 1.05);
            btnWriteRequest.setGraphic(icon);
        }
        if (btnStressTest != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_ZAP, "#fbbf24", 1.05);
            btnStressTest.setGraphic(icon);
        }
        if (btnRescanHardware != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_USB, "#34d399", 0.95);
            btnRescanHardware.setGraphic(icon);
        }
        if (btnSimulatePnp != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_CHECK, "#38bdf8", 0.95);
            btnSimulatePnp.setGraphic(icon);
        }
        if (btnClearLogs != null) {
            SVGPath icon = IconHelper.createIcon(IconHelper.ICON_TRASH, "#94a3b8", 0.9);
            btnClearLogs.setGraphic(icon);
        }

        // Iconos de las cajas de métricas (KPIs)
        if (boxKpi1 != null) {
            boxKpi1.getChildren().setAll(IconHelper.createIcon(IconHelper.ICON_CPU, "#38bdf8", 1.1));
        }
        if (boxKpi2 != null) {
            boxKpi2.getChildren().setAll(IconHelper.createIcon(IconHelper.ICON_QUEUE, "#f59e0b", 1.1));
        }
        if (boxKpi3 != null) {
            boxKpi3.getChildren().setAll(IconHelper.createIcon(IconHelper.ICON_CLOCK, "#10b981", 1.1));
        }
        if (boxKpi4 != null) {
            boxKpi4.getChildren().setAll(IconHelper.createIcon(IconHelper.ICON_ZAP, "#a855f7", 1.1));
        }
    }

    /**
     * Configura el ListView con una fábrica de celdas gráficas personalizadas (Custom Card Cells).
     */
    private void setupQueueListView() {
        if (queueListView == null) {
            return;
        }

        VBox emptyPlaceholder = new VBox(8);
        emptyPlaceholder.setAlignment(Pos.CENTER);
        SVGPath emptyIcon = IconHelper.createIcon(IconHelper.ICON_QUEUE, "#475569", 1.4);
        Label emptyLabel = new Label("Cola FIFO vacía\nListo para recibir peticiones o eventos de hardware...");
        emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-text-alignment: center;");
        emptyPlaceholder.getChildren().addAll(emptyIcon, emptyLabel);
        queueListView.setPlaceholder(emptyPlaceholder);

        queueListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(IORequest req, boolean empty) {
                super.updateItem(req, empty);
                if (empty || req == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);
                    setGraphic(createQueueRequestCard(req));
                    setStyle("-fx-background-color: transparent; -fx-padding: 3px 0;");
                }
            }
        });
    }

    /**
     * Construye la tarjeta visual para un elemento encolado en el Gestor de Colas.
     */
    private HBox createQueueRequestCard(IORequest req) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("queue-card");

        boolean isRead = req.getOperationType() == OperationType.LECTURA;
        String badgeTextColor = isRead ? "#38bdf8" : "#c084fc";
        String iconPath = isRead ? IconHelper.ICON_READ : IconHelper.ICON_WRITE;

        HBox badgeBox = new HBox(5);
        badgeBox.setAlignment(Pos.CENTER);
        badgeBox.setPadding(new Insets(4, 8, 4, 8));
        badgeBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 6px;",
                isRead ? "rgba(2, 132, 199, 0.22)" : "rgba(124, 58, 237, 0.22)"));

        SVGPath badgeIcon = IconHelper.createIcon(iconPath, badgeTextColor, 0.8);
        Label badgeLabel = new Label(isRead ? "READ" : "WRITE");
        badgeLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px;", badgeTextColor));
        badgeBox.getChildren().addAll(badgeIcon, badgeLabel);

        VBox infoBox = new VBox(2);
        Label idLabel = new Label(req.getId());
        idLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        IODevice targetDevice = subsystem.findDeviceById(req.getTargetDeviceId());
        String targetName = targetDevice != null ? targetDevice.getName() : req.getTargetDeviceId();
        Label targetLabel = new Label("Destino: " + targetName);
        targetLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        infoBox.getChildren().addAll(idLabel, targetLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label payloadBadge = new Label(req.getPayloadSize() + " KB");
        payloadBadge.setStyle("-fx-background-color: #334155; -fx-text-fill: #f1f5f9; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 3px 8px;");

        card.getChildren().addAll(badgeBox, infoBox, spacer, payloadBadge);
        return card;
    }

    /**
     * Genera una petición de E/S considerando la selección del usuario o distribución equilibrada entre dispositivos activos.
     */
    private void handleGenerateRequest(OperationType type) {
        List<IODevice> onlineDevices = subsystem.getDevices().stream()
                .filter(d -> d.getStatus() != DeviceStatus.APAGADO)
                .toList();
        if (onlineDevices.isEmpty()) {
            return;
        }

        IODevice target = null;
        if (cmbTargetDevice != null && cmbTargetDevice.getValue() != null) {
            String selected = cmbTargetDevice.getValue();
            if (!selected.startsWith("[Auto")) {
                for (IODevice d : subsystem.getDevices()) {
                    if (selected.contains("(" + d.getId() + ")")) {
                        target = d;
                        break;
                    }
                }
            }
        }

        if (target == null) {
            target = onlineDevices.get(random.nextInt(onlineDevices.size()));
        }

        int payload = 30 + random.nextInt(50);
        String reqId = String.format("REQ-%03d", requestCounter++);
        totalRequestsGenerated++;

        IORequest request = new IORequest(reqId, target.getId(), payload, type);
        subsystem.addRequest(request);
        updateUI();
    }

    /**
     * Actualiza la totalidad de la interfaz de usuario con datos en tiempo real.
     */
    private void updateUI() {
        // 1. Actualizar KPIs Superiores
        if (lblTotalRequests != null) {
            lblTotalRequests.setText(String.valueOf(totalRequestsGenerated));
        }
        if (lblQueueCount != null) {
            lblQueueCount.setText(String.valueOf(subsystem.getRequestQueue().size()));
        }
        if (lblActiveDevices != null) {
            int total = subsystem.getDevices().size();
            long onlineCount = subsystem.getDevices().stream().filter(d -> d.getStatus() != DeviceStatus.APAGADO).count();
            lblActiveDevices.setText(onlineCount + " / " + total + " Activos");
        }

        // 2. Actualizar Gestor de Colas
        if (queueListView != null) {
            queueListView.getItems().setAll(subsystem.getRequestQueue());
        }

        // 3. Actualizar Tarjetas de Hardware
        if (devicesContainer != null) {
            devicesContainer.getChildren().clear();
            for (IODevice device : subsystem.getDevices()) {
                VBox card = createDeviceCard(device);
                devicesContainer.getChildren().add(card);

                // Animación vistosa al activarse en caliente
                if (newlyActivatedDeviceIds.remove(device.getId())) {
                    triggerActivationAnimation(card);
                }

                // Micro-animación de pulso visual cuando el periférico entra en estado INTERRUMPIDO
                DeviceStatus prevStatus = lastKnownDeviceStatus.get(device.getId());
                if (device.getStatus() == DeviceStatus.INTERRUMPIDO && prevStatus != DeviceStatus.INTERRUMPIDO) {
                    triggerPulseAnimation(card);
                }
                lastKnownDeviceStatus.put(device.getId(), device.getStatus());
            }
        }

        // 4. Volcar telemetría en la consola
        if (logConsole != null) {
            List<String> logs = subsystem.getAndClearLogs();
            for (String log : logs) {
                logConsole.appendText(log + "\n");
            }
        }
    }

    /**
     * Construye una tarjeta gráfica futurista para un controlador de hardware con soporte físico y PnP.
     */
    private VBox createDeviceCard(IODevice device) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("device-card");

        String borderColor;
        String badgeText;
        String badgeBg;
        String badgeTextColor;
        String iconColor;

        if (device.getStatus() == DeviceStatus.APAGADO) {
            borderColor = "#24324a";
            badgeText = "○ STANDBY (APAGADO)";
            badgeBg = "rgba(36, 50, 74, 0.4)";
            badgeTextColor = "#94a3b8";
            iconColor = "#475569";
            card.getStyleClass().add("device-offline");
        } else if (device.getStatus() == DeviceStatus.INTERRUMPIDO) {
            borderColor = "#ef4444";
            badgeText = "● ISR PENDIENTE";
            badgeBg = "rgba(239, 68, 68, 0.2)";
            badgeTextColor = "#fca5a5";
            iconColor = "#ef4444";
            card.getStyleClass().add("device-interrupted");
        } else if (device.getStatus() == DeviceStatus.OCUPADO) {
            borderColor = "#f59e0b";
            badgeText = "⚡ OCUPADO";
            badgeBg = "rgba(245, 158, 11, 0.2)";
            badgeTextColor = "#fde68a";
            iconColor = "#f59e0b";
            card.getStyleClass().add("device-busy");
        } else {
            borderColor = "#10b981";
            badgeText = "● ONLINE (ACTIVO)";
            badgeBg = "rgba(16, 185, 129, 0.16)";
            badgeTextColor = "#6ee7b7";
            iconColor = "#10b981";
            card.getStyleClass().add("device-free");
        }

        // Encabezado con Icono de Hardware, Nombre y Badges
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(32, 32);
        iconBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8px;",
                badgeBg));

        String svgPath = IconHelper.getDeviceIconPath(device.getName() + " " + device.getType());
        SVGPath deviceIcon = IconHelper.createIcon(svgPath, iconColor, 0.95);
        iconBox.getChildren().add(deviceIcon);

        VBox titleBox = new VBox(1);
        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(device.getName());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Badge de Hardware Físico o PnP
        Label hwTypeBadge = new Label(device.isRemovable() ? "⚡ USB PnP" : (device.isPhysical() ? "HARDWARE REAL" : "VIRTUAL"));
        hwTypeBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 9px; -fx-font-weight: bold; " +
                "-fx-padding: 2px 5px; -fx-background-radius: 4px;",
                device.isRemovable() ? "rgba(16, 185, 129, 0.2)" : "rgba(56, 189, 248, 0.2)",
                device.isRemovable() ? "#6ee7b7" : "#38bdf8"));
        nameRow.getChildren().addAll(nameLabel, hwTypeBadge);

        String subInfo = device.getExtraInfo() != null && !device.getExtraInfo().isEmpty()
                ? device.getExtraInfo()
                : "ID: " + device.getId() + " • " + device.getType();
        Label idLabel = new Label(subInfo);
        idLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        titleBox.getChildren().addAll(nameRow, idLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(badgeText);
        statusBadge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 10px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 3px 8px; " +
                "-fx-border-color: %s; -fx-border-radius: 6px; -fx-border-width: 1px;",
                badgeBg, badgeTextColor, borderColor));

        header.getChildren().addAll(iconBox, titleBox, spacer, statusBadge);

        // Barra de progreso y cálculo de transferencia
        double progress = 0.0;
        String infoText = "Sin petición activa";
        int percent = 0;

        if (device.getCurrentRequest() != null) {
            int target = device.getCurrentRequest().getPayloadSize();
            progress = Math.min(1.0, (double) device.getCurrentBuffer() / target);
            percent = (int) Math.round(progress * 100);
            infoText = String.format("Req: %s [%s] • %d / %d KB",
                    device.getCurrentRequest().getId(),
                    device.getCurrentRequest().getOperationType(),
                    device.getCurrentBuffer(),
                    target);
        } else if (device.getLastLatencyMs() > 0) {
            infoText = String.format("Última E/S física: %.2f ms", device.getLastLatencyMs());
            if (device.getLastThroughputMBs() > 0) {
                infoText += String.format(" (%.1f MB/s)", device.getLastThroughputMBs());
            }
        }

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setStyle(String.format("-fx-accent: %s;", borderColor));

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label infoLabel = new Label(infoText);
        infoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label percentBadge = new Label(percent > 0 ? percent + "%" : "");
        percentBadge.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 11px;", iconColor));

        footer.getChildren().addAll(infoLabel, footerSpacer, percentBadge);

        card.getChildren().addAll(header, progressBar, footer);
        return card;
    }

    /**
     * Aplica un sutil efecto de pulso (Scale Transition) cuando ocurre una interrupción de hardware.
     */
    private void triggerPulseAnimation(VBox node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(250), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.03);
        st.setToY(1.03);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /**
     * Aplica una animación vistosa de activación en caliente (escala, resorte elástico y resplandor neón).
     */
    private void triggerActivationAnimation(VBox card) {
        card.getStyleClass().add("device-activated");

        ScaleTransition st = new ScaleTransition(Duration.millis(260), card);
        st.setFromX(0.92);
        st.setFromY(0.92);
        st.setToX(1.05);
        st.setToY(1.05);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.millis(1400));
            pause.setOnFinished(ev -> card.getStyleClass().remove("device-activated"));
            pause.play();
        });
        st.play();
    }
}
