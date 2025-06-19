package com.example;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StockTracker {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter stock symbol (e.g., AAPL): ");
        String symbol = scanner.nextLine();

        String apiKey = "3357692a8f8b4a3c9dee4c1b2e1dbd17";  // Replace with your actual key
        String urlString = "https://api.twelvedata.com/price?symbol=" + symbol + "&apikey=" + apiKey;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            String price = json.get("price").getAsString();
            System.out.println("Current price of " + symbol + ": $" + price);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
