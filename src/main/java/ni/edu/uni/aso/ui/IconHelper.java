package ni.edu.uni.aso.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Proveedor centralizado de iconos vectoriales SVG de alta definición para JavaFX.
 * Permite renderizado nítido e independiente de resolución sin librerías externas.
 */
public final class IconHelper {

    private IconHelper() {
        // Clase de utilidades
    }

    // Rutas de vectores SVG (Lucide / Material Design)
    public static final String ICON_READ = "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
    public static final String ICON_WRITE = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String ICON_ZAP = "M7 2v11h3v9l7-12h-4l4-8z";
    public static final String ICON_DISK = "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z";
    public static final String ICON_USB = "M15 7v4h1v2h-3V5h2l-3-4-3 4h2v8H8v-2.07c.7-.37 1.2-1.08 1.2-1.93 0-1.21-.99-2.2-2.2-2.2-1.21 0-2.2.99-2.2 2.2 0 .85.5 1.56 1.2 1.93V13c0 1.11.89 2 2 2h3v3.05c-.71.37-1.2 1.1-1.2 1.95a2.2 2.2 0 0 0 4.4 0c0-.85-.49-1.58-1.2-1.95V15h3c1.11 0 2-.89 2-2v-2h1V7h-4z";
    public static final String ICON_KEYBOARD = "M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z";
    public static final String ICON_MOUSE = "M12 2C8.69 2 6 4.69 6 8v8c0 3.31 2.69 6 6 6s6-2.69 6-6V8c0-3.31-2.69-6-6-6zm4 14c0 2.21-1.79 4-4 4s-4-1.79-4-4v-1h8v1zm0-3H8V8c0-2.21 1.79-4 4-4s4 1.79 4 4v5zm-5-5h2v3h-2V8z";
    public static final String ICON_PRINTER = "M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z";
    public static final String ICON_MONITOR = "M20 3H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h6l-2 3v1h8v-1l-2-3h6c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 13H4V5h16v11z";
    public static final String ICON_TRASH = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    public static final String ICON_QUEUE = "M4 6h16V4H4v2zm0 5h16V9H4v2zm0 5h16v-2H4v2zm0 5h16v-2H4v2z";
    public static final String ICON_CPU = "M6 2v2H4c-1.1 0-2 .9-2 2v2H0v2h2v2H0v2h2v2c0 1.1.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-.9 2-2v-2h2v-2h-2v-2h2v-2h-2V6c0-1.1-.9-2-2-2h-2V2h-2v2h-2V2H6zm0 4h12v12H6V6zm2 2v8h8V8H8z";
    public static final String ICON_CLOCK = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
    public static final String ICON_CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
    public static final String ICON_ALERT = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";

    /**
     * Crea un nodo SVGPath con el color y escala especificados.
     */
    public static SVGPath createIcon(String svgPath, String hexColor, double scale) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setFill(Color.web(hexColor));
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    /**
     * Retorna el trazado SVG correspondiente según el tipo o nombre de periférico.
     */
    public static String getDeviceIconPath(String deviceType) {
        if (deviceType == null) {
            return ICON_CPU;
        }
        String lower = deviceType.toLowerCase();
        if (lower.contains("usb")) {
            return ICON_USB;
        } else if (lower.contains("disco")) {
            return ICON_DISK;
        } else if (lower.contains("teclado")) {
            return ICON_KEYBOARD;
        } else if (lower.contains("mouse") || lower.contains("ratón") || lower.contains("puntero")) {
            return ICON_MOUSE;
        } else if (lower.contains("impresora")) {
            return ICON_PRINTER;
        } else if (lower.contains("monitor") || lower.contains("pantalla")) {
            return ICON_MONITOR;
        }
        return ICON_CPU;
    }
}
