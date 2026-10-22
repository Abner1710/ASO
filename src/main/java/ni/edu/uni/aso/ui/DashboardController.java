package ni.edu.uni.aso.ui;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
import ni.edu.uni.aso.core.IODevice;
import ni.edu.uni.aso.core.IORequest;
import ni.edu.uni.aso.core.IOSubsystem;
import ni.edu.uni.aso.core.OperationType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Controlador principal del Dashboard de Simulación de Entrada/Salida.
 * Diseñado con estética futurista de alta fidelidad, micro-animaciones e iconos vectoriales SVG.
 */
public class DashboardController {

    // Controles FXML - Botones y Formularios
    @FXML private Button btnReadRequest;
    @FXML private Button btnWriteRequest;
    @FXML private ToggleButton btnStressTest;
    @FXML private Button btnClearLogs;

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
    private Timeline stressTimeline;
    private final Map<String, DeviceStatus> lastKnownDeviceStatus = new HashMap<>();

    @FXML
    public void initialize() {
        setupIcons();
        setupQueueListView();

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

        // Estado vacío de la cola
        VBox emptyPlaceholder = new VBox(8);
        emptyPlaceholder.setAlignment(Pos.CENTER);
        SVGPath emptyIcon = IconHelper.createIcon(IconHelper.ICON_QUEUE, "#475569", 1.4);
        Label emptyLabel = new Label("Cola FIFO vacía\nEsperando nuevas peticiones del kernel...");
        emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-text-alignment: center;");
        emptyPlaceholder.getChildren().addAll(emptyIcon, emptyLabel);
        queueListView.setPlaceholder(emptyPlaceholder);

        // Fábrica de celdas gráficas personalizadas
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
        String badgeColor = isRead ? "#0284c7" : "#7c3aed";
        String badgeTextColor = isRead ? "#38bdf8" : "#c084fc";
        String iconPath = isRead ? IconHelper.ICON_READ : IconHelper.ICON_WRITE;

        // Badge de Operación con Icono
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

        // Información de la Petición
        VBox infoBox = new VBox(2);
        Label idLabel = new Label(req.getId());
        idLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        IODevice targetDevice = subsystem.findDeviceById(req.getTargetDeviceId());
        String targetName = targetDevice != null ? targetDevice.getName() : req.getTargetDeviceId();
        Label targetLabel = new Label("Destino: " + targetName + " (" + req.getTargetDeviceId() + ")");
        targetLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        infoBox.getChildren().addAll(idLabel, targetLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de Payload
        Label payloadBadge = new Label(req.getPayloadSize() + " B");
        payloadBadge.setStyle("-fx-background-color: #334155; -fx-text-fill: #f1f5f9; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 3px 8px;");

        card.getChildren().addAll(badgeBox, infoBox, spacer, payloadBadge);
        return card;
    }

    /**
     * Genera una petición de E/S aleatoria y actualiza el conteo global.
     */
    private void handleGenerateRequest(OperationType type) {
        List<IODevice> devices = subsystem.getDevices();
        if (devices.isEmpty()) {
            return;
        }

        IODevice target = devices.get(random.nextInt(devices.size()));
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
            long activeCount = subsystem.getDevices().stream()
                    .filter(d -> d.getStatus() != DeviceStatus.LIBRE)
                    .count();
            lblActiveDevices.setText(activeCount + " / " + subsystem.getDevices().size());
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
     * Construye una tarjeta gráfica futurista para un controlador de hardware con icono SVG.
     */
    private VBox createDeviceCard(IODevice device) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.getStyleClass().add("device-card");

        String borderColor;
        String badgeText;
        String badgeBg;
        String badgeTextColor;
        String iconColor;

        if (device.getStatus() == DeviceStatus.INTERRUMPIDO) {
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
            badgeText = "● LIBRE";
            badgeBg = "rgba(16, 185, 129, 0.16)";
            badgeTextColor = "#6ee7b7";
            iconColor = "#10b981";
            card.getStyleClass().add("device-free");
        }

        // Encabezado con Icono de Hardware y Nombre
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Contenedor de Icono SVG específico para el periférico
        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(32, 32);
        iconBox.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8px;",
                badgeBg));

        String svgPath = IconHelper.getDeviceIconPath(device.getName());
        SVGPath deviceIcon = IconHelper.createIcon(svgPath, iconColor, 0.95);
        iconBox.getChildren().add(deviceIcon);

        VBox titleBox = new VBox(1);
        Label nameLabel = new Label(device.getName());
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label idLabel = new Label("ID: " + device.getId() + " • " + device.getType());
        idLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        titleBox.getChildren().addAll(nameLabel, idLabel);

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
        String infoText = "Sin petición activa • Buffer: 0 / " + device.getBufferSize() + " B";
        int percent = 0;

        if (device.getCurrentRequest() != null) {
            int target = device.getCurrentRequest().getPayloadSize();
            progress = Math.min(1.0, (double) device.getCurrentBuffer() / target);
            percent = (int) Math.round(progress * 100);
            infoText = String.format("Req: %s [%s] • %d / %d B (%d%%)",
                    device.getCurrentRequest().getId(),
                    device.getCurrentRequest().getOperationType(),
                    device.getCurrentBuffer(),
                    target,
                    percent);
        }

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(9);
        progressBar.setStyle(String.format("-fx-accent: %s;", borderColor));

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label infoLabel = new Label(infoText);
        infoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label percentBadge = new Label(percent + "%");
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
}
