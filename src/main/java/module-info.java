module ni.edu.uni.aso {
    requires javafx.controls;
    requires javafx.fxml;

    opens ni.edu.uni.aso.ui to javafx.fxml;
    opens ni.edu.uni.aso.core to javafx.base;

    exports ni.edu.uni.aso;
    exports ni.edu.uni.aso.core;
    exports ni.edu.uni.aso.ui;
}
