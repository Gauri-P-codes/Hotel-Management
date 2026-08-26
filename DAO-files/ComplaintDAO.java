import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {

    public boolean insertComplaint(String guestName, long phno, int roomNo, String department,
                                   String description, boolean status) {
        String sql = "INSERT INTO complaints (guest_name, phno, room_no, department, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, guestName);
            ps.setLong(2, phno);
            ps.setInt(3, roomNo);
            ps.setString(4, department);
            ps.setString(5, description);
            ps.setString(6, status ? "open" : "resolved");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Insert complaint failed: " + e.getMessage());
            return false;
        }
    }

    public List<String> getAllComplaints() {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM complaints";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(rs.getString("guest_name") + " | Room " + rs.getInt("room_no")
                        + " | " + rs.getString("department") + " | " + rs.getString("description")
                        + " | " + rs.getString("status"));
            }
        } catch (SQLException e) {
            System.out.println("Fetch complaints failed: " + e.getMessage());
        }
        return results;
    }
}