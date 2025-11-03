module com.example.proj_graf02 {
    requires javafx.controls;
    requires javafx.graphics;

    requires java.desktop;   // ImageIO/BufferedImage

    requires javafx.fxml;             // zostaw tylko jeśli używasz FXML
    opens com.example.proj_graf02 to javafx.fxml; // j.w.

    exports com.example.proj_graf02;
}