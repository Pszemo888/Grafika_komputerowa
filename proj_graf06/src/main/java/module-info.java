module com.example.proj_graf06 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf06 to javafx.fxml;
    exports com.example.proj_graf06;
}