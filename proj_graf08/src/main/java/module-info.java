module com.example.proj_graf08 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.proj_graf08 to javafx.fxml;
    exports com.example.proj_graf08;
}