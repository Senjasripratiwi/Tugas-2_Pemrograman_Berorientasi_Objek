package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;

public class Main {
    private static final String API_KEY = "items";
    private static final int PORT = 9014;
    private static final int FRIEND_PORT = 9022;

    public static void main(String[] args) throws Exception {
        Database.createTables();


        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new HttpRequestHandler(API_KEY));
        server.setExecutor(null);
        server.start();
        System.out.printf("Server Anda berjalan di port %d...\n", PORT);


        HttpServer friendServer = HttpServer.create(new InetSocketAddress(FRIEND_PORT), 0);
        friendServer.createContext("/", new HttpRequestHandler(API_KEY));
        friendServer.setExecutor(null);
        friendServer.start();
        System.out.printf("Server teman Anda berjalan di port %d...\n", FRIEND_PORT);
    }

    static class Database {
        private static final String DB_URL = "jdbc:sqlite:C:/Users/SENJA/AppData/Roaming/Microsoft/Windows/Network Shortcuts/sistem pembayaran subscription.db";

        public static Connection connect() {
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(DB_URL);
            } catch (SQLException e) {
                System.out.println("Koneksi database gagal: " + e.getMessage());
            }
            return conn;
        }

        public static void createTables() {
            String[] tables = {
                    "CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY, email TEXT NOT NULL, first_name TEXT NOT NULL, last_name TEXT NOT NULL, phone_number TEXT NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS shipping_addresses (id INTEGER PRIMARY KEY, customer INTEGER NOT NULL, title TEXT NOT NULL, line1 TEXT NOT NULL, line2 TEXT, city TEXT NOT NULL, province TEXT NOT NULL, postcode TEXT NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS subscriptions (id INTEGER PRIMARY KEY, customer INTEGER NOT NULL, billing_period INTEGER NOT NULL, billing_period_unit TEXT NOT NULL, total_due INTEGER NOT NULL, activated_at TEXT NOT NULL, current_term_start TEXT NOT NULL, current_term_end TEXT NOT NULL, status TEXT NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS subscription_items (subscription INTEGER, item INTEGER, quantity INTEGER NOT NULL, price INTEGER NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(subscription, item))",
                    "CREATE TABLE IF NOT EXISTS items (id INTEGER PRIMARY KEY, name TEXT NOT NULL, price INTEGER NOT NULL, type TEXT NOT NULL, is_active INTEGER NOT NULL)",
                    "CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY, customer INTEGER NOT NULL, card_type TEXT NOT NULL, masked_number TEXT NOT NULL, expiry_month INTEGER NOT NULL, expiry_year INTEGER NOT NULL, status TEXT NOT NULL, is_primary INTEGER NOT NULL)"
            };

            try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
                for (String table : tables) {
                    stmt.execute(table);
                }
            } catch (SQLException e) {
                System.out.println("Error saat membuat tabel: " + e.getMessage());
            }
        }
    }

    static class HttpRequestHandler implements HttpHandler {
        private final String apiKey;

        public HttpRequestHandler(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();


            String requestApiKey = exchange.getRequestHeaders().getFirst("items");
            if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            switch (requestMethod) {
                case "GET":
                    HttpGetHandler.handle(exchange, path, query);
                    break;
                case "POST":
                    HttpPostHandler.handle(exchange, path);
                    break;
                case "PUT":
                    HttpPutHandler.handle(exchange, path);
                    break;
                case "DELETE":
                    HttpDeleteHandler.handle(exchange, path);
                    break;
                default:
                    sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                    break;
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class HttpGetHandler {

        public static void handle(HttpExchange exchange, String path, String query) throws IOException {
            if (path.equals("/items")) {
                getAllItems(exchange);
            } else if (path.startsWith("/items/")) {
                getItemDetail(exchange, path);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        }

        private static void getAllItems(HttpExchange exchange) throws IOException {
            String sql = "SELECT * FROM items";
            List<Map<String, Object>> items = new ArrayList<>();
            try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("name", rs.getString("name"));
                    item.put("price", rs.getInt("price"));
                    item.put("type", rs.getString("type"));
                    item.put("is_active", rs.getInt("is_active"));
                    items.add(item);
                }
            } catch (SQLException e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            String response = mapper.writeValueAsString(items);
            sendResponse(exchange, 200, response);
        }

        private static void getItemDetail(HttpExchange exchange, String path) throws IOException {
            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
                return;
            }

            int itemId = Integer.parseInt(parts[2]);
            String query = "SELECT * FROM items WHERE id = " + itemId;

            try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("name", rs.getString("name"));
                    item.put("price", rs.getInt("price"));
                    item.put("type", rs.getString("type"));
                    item.put("is_active", rs.getInt("is_active"));

                    ObjectMapper mapper = new ObjectMapper();
                    String response = mapper.writeValueAsString(item);
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Item Not Found\"}");
                }
            } catch (SQLException e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }

        private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class HttpPostHandler {

        public static void handle(HttpExchange exchange, String path) throws IOException {
            switch (path) {
                case "/items":
                    createItem(exchange);
                    break;
                default:
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                    break;
            }
        }

        private static void createItem(HttpExchange exchange) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            InputStream requestBody = exchange.getRequestBody();
            Map<String, Object> requestBodyMap = mapper.readValue(requestBody, Map.class);

            String name = (String) requestBodyMap.get("name");
            int price = (int) requestBodyMap.get("price");
            String type = (String) requestBodyMap.get("type");
            int isActive = (int) requestBodyMap.get("is_active");

            if (name == null || type == null) {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
                return;
            }

            String sql = "INSERT INTO items (name, price, type, is_active) VALUES (?, ?, ?, ?)";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, price);
                pstmt.setString(3, type);
                pstmt.setInt(4, isActive);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int itemId = generatedKeys.getInt(1);
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", itemId);
                            item.put("name", name);
                            item.put("price", price);
                            item.put("type", type);
                            item.put("is_active", isActive);

                            String response = mapper.writeValueAsString(item);
                            sendResponse(exchange, 201, response);
                        }
                    }
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to create item\"}");
                }
            } catch (SQLException e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }

        private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class HttpPutHandler {

        public static void handle(HttpExchange exchange, String path) throws IOException {
            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
                return;
            }

            int itemId = Integer.parseInt(parts[2]);
            updateItem(exchange, itemId);
        }

        private static void updateItem(HttpExchange exchange, int itemId) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            InputStream requestBody = exchange.getRequestBody();
            Map<String, Object> requestBodyMap = mapper.readValue(requestBody, Map.class);

            String name = (String) requestBodyMap.get("name");
            int price = (int) requestBodyMap.get("price");
            String type = (String) requestBodyMap.get("type");
            int isActive = (int) requestBodyMap.get("is_active");

            if (name == null || type == null) {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
                return;
            }

            String sql = "UPDATE items SET name = ?, price = ?, type = ?, is_active = ? WHERE id = ?";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setInt(2, price);
                pstmt.setString(3, type);
                pstmt.setInt(4, isActive);
                pstmt.setInt(5, itemId);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", itemId);
                    item.put("name", name);
                    item.put("price", price);
                    item.put("type", type);
                    item.put("is_active", isActive);

                    String response = mapper.writeValueAsString(item);
                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Item Not Found\"}");
                }
            } catch (SQLException e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }

        private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class HttpDeleteHandler {

        public static void handle(HttpExchange exchange, String path) throws IOException {
            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendResponse(exchange, 400, "{\"error\":\"Bad Request\"}");
                return;
            }

            int itemId = Integer.parseInt(parts[2]);
            deleteItem(exchange, itemId);
        }

        private static void deleteItem(HttpExchange exchange, int itemId) throws IOException {
            String sql = "DELETE FROM items WHERE id = ?";
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, itemId);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    sendResponse(exchange, 204, ""); // No Content
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Item Not Found\"}");
                }
            } catch (SQLException e) {
                sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }

        private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
