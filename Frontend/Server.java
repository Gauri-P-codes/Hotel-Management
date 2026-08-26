import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Server {

    static RoomAvailabilityService availabilityService = new RoomAvailabilityService();
    static BookingDAO dao = new BookingDAO();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/rooms/available", new AvailabilityHandler());
        server.createContext("/bookings", new BookingsHandler()); // GET = list all, POST = create

        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    // ---- GET /rooms/available?type=guest_room&checkin=2026-09-01&days=3 ----
    static class AvailabilityHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String type = params.get("type");
            LocalDate checkIn = LocalDate.parse(params.get("checkin"));
            int days = Integer.parseInt(params.get("days"));
            LocalDate checkOut = checkIn.plusDays(days);

            int room = availabilityService.findAvailableRoom(type, checkIn, checkOut);
            String json = "{\"room\":" + room + "}";
            sendResponse(exchange, 200, json);
        }
    }

    // ---- GET /bookings (list all) | POST /bookings (create) ----
    static class BookingsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                String json = dao.getAllBookingsAsJson();
                sendResponse(exchange, 200, json);

            } else if (method.equalsIgnoreCase("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseQuery(body);

                String name = params.get("name");
                long phno = Long.parseLong(params.get("phno"));
                String type = params.get("type");
                LocalDate checkIn = LocalDate.parse(params.get("checkin"));
                int days = Integer.parseInt(params.get("days"));

                int room = availabilityService.findAvailableRoom(type, checkIn, checkIn.plusDays(days));

                String json;
                if (room > -1) {
                    boolean saved = dao.insertBooking(name, phno, room, type, checkIn, days, "ONLINE");
                    json = saved
                            ? "{\"success\":true,\"room\":" + room + "}"
                            : "{\"success\":false,\"message\":\"DB insert failed\"}";
                } else {
                    json = "{\"success\":false,\"message\":\"No room available\"}";
                }
                sendResponse(exchange, 200, json);

            } else if (method.equalsIgnoreCase("DELETE")) {
                // NEW: DELETE /bookings?name=..&phno=..&room=..
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String name = params.get("name");
                long phno = Long.parseLong(params.get("phno"));
                int room = Integer.parseInt(params.get("room"));

                boolean deleted = dao.deleteBooking(name, phno, room);
                String json = deleted
                        ? "{\"success\":true}"
                        : "{\"success\":false,\"message\":\"Booking not found\"}";
                sendResponse(exchange, 200, json);
            }
        }
    }

    // ---- Helpers ----
    static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    static void sendResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }
}
