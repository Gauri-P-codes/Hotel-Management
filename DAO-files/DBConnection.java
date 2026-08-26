import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/hotel_mgmt?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";	private static final String username="root";
    private static final String password="dbms@2025";
    private static Connection connection=null;

    public static Connection getConnection() {
        try {
            if(connection==null || connection.isClosed())
            {
                connection=DriverManager.getConnection(URL, username, password);
            }
        }
        catch(SQLException sqe) {
            System.out.println("Connection failed: " + sqe.getMessage());
        }
        return connection;
    }
    public static void main(String[] args) {
                // TODO Auto-generated method stub
                connection=getConnection();
                if(connection==null)
                {
                    System.out.println("Connection failed!");
                }
                else {
                    System.out.println("Connection successful!");
                }


    }
}
