module com.example.proj_graf03 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf03 to javafx.fxml;
    exports com.example.proj_graf03;
}