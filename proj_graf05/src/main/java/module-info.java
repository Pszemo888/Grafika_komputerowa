module com.example.proj_graf05 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf05 to javafx.fxml;
    exports com.example.proj_graf05;
}