module com.btl.n8 {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;
    requires com.google.gson;

    opens com.btl.n8 to javafx.fxml;
    opens com.btl.n8.Controller to javafx.fxml;

    // Thêm để Gson có thể access fields
    opens com.btl.n8.DTO to com.google.gson;
    opens com.btl.n8.Network to com.google.gson;
    opens com.btl.n8.Service to com.google.gson;

    exports com.btl.n8;
    exports com.btl.n8.Controller;
    exports com.btl.n8.DTO;
    exports com.btl.n8.Network;
    exports com.btl.n8.Util;
    exports com.btl.n8.Service;
    exports com.btl.n8.Connection;
    exports com.btl.n8.Model.Entity;
    opens com.btl.n8.Model.Entity to com.google.gson;
    exports com.btl.n8.Model.Enums;
    opens com.btl.n8.Model.Enums to com.google.gson;
    exports com.btl.n8.Model.Mapper;
    opens com.btl.n8.Model.Mapper to com.google.gson;
}