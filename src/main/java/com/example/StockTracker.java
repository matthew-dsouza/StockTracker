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

    static final String CSV_FILE = "prices.csv";
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String apiKey = ConfigLoader.getApiKey();

        // 🔧 Let user set refresh interval
        System.out.print("Enter refresh interval in seconds (default 20): ");
        String refreshInput = scanner.nextLine();
        int refreshInterval = 20; // default

        try {
            if (!refreshInput.isEmpty()) {
                refreshInterval = Integer.parseInt(refreshInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Using default 20 seconds.");
        }

        // 🔧 Input stock symbols
        System.out.print("Enter stock symbols separated by commas (e.g., AAPL,GOOGL,MSFT): ");
        String input = scanner.nextLine();
        String[] symbols = input.split(",");

        // 🔔 Setup price alerts
        Map<String, Double> alertThresholds = new HashMap<>();
        for (String rawSymbol : symbols) {
            String symbol = rawSymbol.trim().toUpperCase();
            System.out.print("Set price alert for " + symbol + " (leave blank to skip): ");
            String alertInput = scanner.nextLine();
            if (!alertInput.isEmpty()) {
                try {
                    double alertPrice = Double.parseDouble(alertInput);
                    alertThresholds.put(symbol, alertPrice);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Skipping alert for " + symbol);
                }
            }
        }

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

                    // 🧠 Calculate percent change
                    String percentChangeText = "-";
                    double changePercent = 0;
                    if (lastPrice > 0) {
                        changePercent = ((price - lastPrice) / lastPrice) * 100;
                        percentChangeText = String.format("%.2f%%", changePercent);
                    }

                    String arrow = getStatusArrow(price, lastPrice);
                    String priceFormatted = String.format("%.2f USD", price);

                    // 🚨 Price alert check
                    if (alertThresholds.containsKey(symbol)) {
                        double alertPrice = alertThresholds.get(symbol);
                        if (price >= alertPrice) {
                            System.out.println("\u001B[33m[ALERT]\u001B[0m " + symbol + " has reached $" + price + " (Target: $" + alertPrice + ")");
                        }
                    }

                    // 📺 Console display
                    System.out.println(symbol + ": $" + priceFormatted + " " + arrow + " | Change: " + percentChangeText);

                    // 📝 CSV logging
                    appendToCsv(CSV_FILE, symbol, priceFormatted, percentChangeText);

                } catch (Exception e) {
                    System.out.println(symbol + ": Error - " + e.getMessage());
                }
            }

            try {
                Thread.sleep(refreshInterval * 1000L);
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
                writer.println("Date,Time,Symbol,Price (USD),% Change");
            } catch (IOException e) {
                System.out.println("Failed to write CSV header: " + e.getMessage());
            }
        }
    }

    private static void appendToCsv(String fileName, String symbol, String price, String percentChange) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.println(time + "," + symbol + "," + price + "," + percentChange);
        } catch (IOException e) {
            System.out.println("Failed to write to CSV: " + e.getMessage());
        }
    }
}
