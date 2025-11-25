module com.example.proj_graf09 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf09 to javafx.fxml;
    exports com.example.proj_graf09;
}