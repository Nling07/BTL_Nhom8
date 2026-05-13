module com.btl.n8 {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires com.google.gson;

    opens com.btl.n8 to javafx.fxml;
    opens com.btl.n8.Controller to javafx.fxml;

    // Thêm để Gson có thể access fields
    opens com.btl.n8.dto to com.google.gson;
    opens com.btl.n8.Model to com.google.gson;
    opens com.btl.n8.network to com.google.gson;
    opens com.btl.n8.Service to com.google.gson;

    exports com.btl.n8;
    exports com.btl.n8.Controller;
    exports com.btl.n8.dto;
    exports com.btl.n8.network;
    exports com.btl.n8.util;
    exports com.btl.n8.Model;
    exports com.btl.n8.Service;
    exports com.btl.n8.Connection;
}