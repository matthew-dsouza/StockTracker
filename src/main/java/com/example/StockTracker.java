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

        String apiKey = ConfigLoader.getApiKey();

        System.out.print("Enter stock symbols separated by commas (e.g., AAPL,GOOGL,MSFT): ");
        String input = scanner.nextLine();

        String[] symbols = input.split(",");

        for (String rawSymbol : symbols) {
            String symbol = rawSymbol.trim().toUpperCase();

            String urlString = "https://api.twelvedata.com/price?symbol=" + symbol + "&apikey=" + apiKey;

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("price")) {
                    String price = json.get("price").getAsString();
                    System.out.println(symbol + ": $" + price);
                } else if (json.has("message")) {
                    System.out.println(symbol + ": Error - " + json.get("message").getAsString());
                }

            } catch (Exception e) {
                System.out.println(symbol + ": Error - " + e.getMessage());
            }
        }
    }
}
