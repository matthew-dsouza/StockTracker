package com.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StockTracker {

    static final int REFRESH_INTERVAL = 30; // in seconds
    static final String CSV_FILE = "prices.csv";
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String apiKey = ConfigLoader.getApiKey();

        System.out.print("Enter stock symbols separated by commas (e.g., AAPL,GOOGL,MSFT): ");
        String input = scanner.nextLine();
        String[] symbols = input.split(",");

        Map<String, Double> lastPrices = new HashMap<>();
        writeCsvHeaderIfNotExists(CSV_FILE);

        while (true) {
            System.out.println("\n--- Stock Prices (" + LocalDateTime.now().format(TIME_FORMAT) + ") ---");

            for (String rawSymbol : symbols) {
                String symbol = rawSymbol.trim().toUpperCase();

                try {
                    double price = fetchStockPrice(symbol, apiKey);
                    double lastPrice = lastPrices.getOrDefault(symbol, -1.0);
                    lastPrices.put(symbol, price);

                    String arrow = getStatusArrow(price, lastPrice);
                    String priceFormatted = String.format("%.2f USD", price);

                    System.out.println(symbol + ": $" + priceFormatted + " " + arrow);
                    appendToCsv(CSV_FILE, symbol, priceFormatted);

                } catch (Exception e) {
                    System.out.println(symbol + ": Error - " + e.getMessage());
                }
            }

            try {
                Thread.sleep(REFRESH_INTERVAL * 1000L);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
                break;
            }
        }
    }

    private static double fetchStockPrice(String symbol, String apiKey) throws Exception {
        String urlString = "https://api.twelvedata.com/price?symbol=" + symbol + "&apikey=" + apiKey;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        if (json.has("price")) {
            return Double.parseDouble(json.get("price").getAsString());
        } else if (json.has("message")) {
            throw new Exception(json.get("message").getAsString());
        }

        throw new Exception("Unexpected response from API");
    }

    private static String getStatusArrow(double price, double lastPrice) {
        if (lastPrice < 0) return "-";
        if (price > lastPrice) return "\u001B[32m↑\u001B[0m"; // Green up
        if (price < lastPrice) return "\u001B[31m↓\u001B[0m"; // Red down
        return "-";
    }

    private static void writeCsvHeaderIfNotExists(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
                writer.println("Time,Symbol,Price (USD)");
            } catch (IOException e) {
                System.out.println("Failed to write CSV header: " + e.getMessage());
            }
        }
    }

    private static void appendToCsv(String fileName, String symbol, String price) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.println(time + "," + symbol + "," + price);
        } catch (IOException e) {
            System.out.println("Failed to write to CSV: " + e.getMessage());
        }
    }
}
