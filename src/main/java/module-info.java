module com.example.btl_nhom8 {

    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires mysql.connector.j;

    opens com.btl.n8 to javafx.fxml;
    exports com.btl.n8;
    exports com.btl.n8.Controller;
    opens com.btl.n8.Controller to javafx.fxml;

}