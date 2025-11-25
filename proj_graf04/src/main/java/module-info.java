module com.example.proj_graf04 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;   // <-- potrzebne dla BufferedImage itd.

    opens com.example.proj_graf04 to javafx.fxml;
    exports com.example.proj_graf04;
}