import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

public class RoomAvailabilityService {

    private BookingDAO dao = new BookingDAO();

    /**
     * Finds the first available room of a given type for the requested date range.
     * Uses a sorted interval map (TreeMap) per room to check overlap in O(log n)
     * instead of scanning every booking linearly.
     */
    public int findAvailableRoom(String roomType, LocalDate checkIn, LocalDate checkOut) {
        List<Integer> candidateRooms = dao.getRoomsByType(roomType);

        for (int roomNo : candidateRooms) {
            if (isRoomFree(roomNo, checkIn, checkOut)) {
                return roomNo; // first free room found
            }
        }
        return -1; // no room available
    }

    /**
     * Checks if a specific room is free for the given date range.
     * Builds a TreeMap of that room's bookings (checkIn -> checkOut),
     * then only checks the immediate neighboring intervals instead of all of them.
     */
    private boolean isRoomFree(int roomNo, LocalDate newIn, LocalDate newOut) {
        List<LocalDate[]> booked = dao.getBookedIntervals(roomNo);

        // Build sorted map: check-in date -> check-out date
        TreeMap<LocalDate, LocalDate> intervals = new TreeMap<>();
        for (LocalDate[] range : booked) {
            intervals.put(range[0], range[1]);
        }

        // Check the booking that starts right before our check-in (could overlap into our stay)
        var floorEntry = intervals.floorEntry(newIn);
        if (floorEntry != null && floorEntry.getValue().isAfter(newIn)) {
            return false; // previous booking's checkout is after our check-in -> overlap
        }

        // Check the booking that starts right after our check-in (could start during our stay)
        var ceilingEntry = intervals.ceilingEntry(newIn);
        if (ceilingEntry != null && ceilingEntry.getKey().isBefore(newOut)) {
            return false; // next booking starts before our check-out -> overlap
        }

        return true; // no overlap with neighbors = room is free
    }

}
