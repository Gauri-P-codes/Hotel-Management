import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class BookingDAO {
    public String getAllBookingsAsJson() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM guests";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append("{")
                        .append("\"name\":\"").append(rs.getString("name")).append("\",")
                        .append("\"phno\":").append(rs.getLong("phno")).append(",")
                        .append("\"room_no\":").append(rs.getInt("room_no")).append(",")
                        .append("\"room_type\":\"").append(rs.getString("room_type")).append("\",")
                        .append("\"check_in_date\":\"").append(rs.getDate("check_in_date")).append("\",")
                        .append("\"check_out_date\":\"").append(rs.getDate("check_out_date")).append("\"")
                        .append("}");
            }
        } catch (SQLException e) {
            System.out.println("Fetch all failed: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }
    public boolean updateRoom(String name, long phno, int oldRoom, int newRoom, String newRoomType) {
        String sql = "UPDATE guests SET room_no=?, room_type=? WHERE name=? AND phno=? AND room_no=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newRoom);
            ps.setString(2, newRoomType);
            ps.setString(3, name);
            ps.setLong(4, phno);
            ps.setInt(5, oldRoom);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Update room failed: " + e.getMessage());
            return false;
        }
    }
    public boolean deleteBooking(String name, long phno, int roomNo) {
        String sql = "DELETE FROM guests WHERE name=? AND phno=? AND room_no=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, phno);
            ps.setInt(3, roomNo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Delete failed: " + e.getMessage());
            return false;
        }
    }
    public boolean freeRoom(int roomNo) {
        String sql = "UPDATE rooms SET status='available' WHERE room_no=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Free room failed: " + e.getMessage());
            return false;
        }
    }
    // Insert a new booking into the guests table
    public boolean insertBooking(String name, long phno, int roomNo, String roomType,
                                 LocalDate checkIn, int days, String modeOfBooking) {
        String sql = "INSERT INTO guests (name, phno, room_no, room_type, check_in_date, check_out_date, no_of_days, mode_of_booking) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDate checkOut = checkIn.plusDays(days);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setLong(2, phno);
            ps.setInt(3, roomNo);
            ps.setString(4, roomType);
            ps.setDate(5, Date.valueOf(checkIn));
            ps.setDate(6, Date.valueOf(checkOut));
            ps.setInt(7, days);
            ps.setString(8, modeOfBooking);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Insert failed: " + e.getMessage());
            return false;
        }
    }

    // Fetch all booked date ranges for a specific room (used by the availability/interval check)
    public List<LocalDate[]> getBookedIntervals(int roomNo) {
        List<LocalDate[]> intervals = new ArrayList<>();
        String sql = "SELECT check_in_date, check_out_date FROM guests WHERE room_no = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomNo);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate in = rs.getDate("check_in_date").toLocalDate();
                LocalDate out = rs.getDate("check_out_date").toLocalDate();
                intervals.add(new LocalDate[]{in, out});
            }

        } catch (SQLException e) {
            System.out.println("Fetch failed: " + e.getMessage());
        }
        return intervals;
    }

    // Get all room numbers of a given type (needed to loop through candidates for availability)
    public List<Integer> getRoomsByType(String roomType) {
        List<Integer> rooms = new ArrayList<>();
        String sql = "SELECT room_no FROM rooms WHERE room_type = ? AND status = 'available'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roomType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rooms.add(rs.getInt("room_no"));
            }

        } catch (SQLException e) {
            System.out.println("Fetch failed: " + e.getMessage());
        }
        return rooms;
    }

    // Search bookings by guest name
    public List<String> searchByName(String name) {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM guests WHERE name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name") + " | Room " + rs.getInt("room_no")
                        + " | " + rs.getDate("check_in_date") + " to " + rs.getDate("check_out_date"));
            }

        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }
        return results;
    }
}
