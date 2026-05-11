module com.btl.n8 {

    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires mysql.connector.j;
    requires com.google.gson;
    opens com.btl.n8 to javafx.fxml;
    exports com.btl.n8;
    exports com.btl.n8.Controller;
    opens com.btl.n8.Controller to javafx.fxml;
    exports com.btl.n8.DTO;
    exports com.btl.n8.Network;
    exports com.btl.n8.util;
    exports com.btl.n8.Model;
    exports com.btl.n8.Service;
    exports com.btl.n8.Connection;

}