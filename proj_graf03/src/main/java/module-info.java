module com.example.proj_graf03 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.example.proj_graf03 to javafx.fxml;
    exports com.example.proj_graf03;
}
