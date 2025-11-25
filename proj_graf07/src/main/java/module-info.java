module com.example.proj_graf07 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf07 to javafx.fxml;
    exports com.example.proj_graf07;
}