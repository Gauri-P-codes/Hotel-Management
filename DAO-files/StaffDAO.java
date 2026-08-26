import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {

    public boolean insertStaff(String name, String authId, String uid, String dept,
                               LocalDate hireDate, double salary, long phno, int shift) {
        String sql = "INSERT INTO staff (name, auth_id, uid, role, hire_date, salary, phno, shift) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, authId);
            ps.setString(3, uid);
            ps.setString(4, dept);
            ps.setDate(5, Date.valueOf(hireDate));
            ps.setDouble(6, salary);
            ps.setLong(7, phno);
            ps.setInt(8, shift);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Insert staff failed: " + e.getMessage());
            return false;
        }
    }

    public List<String> getAllStaff() {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM staff";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(rs.getString("name") + " | " + rs.getString("uid") + " | "
                        + rs.getString("role") + " | Shift " + rs.getInt("shift"));
            }
        } catch (SQLException e) {
            System.out.println("Fetch staff failed: " + e.getMessage());
        }
        return results;
    }
}
