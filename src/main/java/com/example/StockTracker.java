package com.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class StockTracker extends Application {

    private TableView<StockData> table;
    private final Map<String, Double> lastPrices = new HashMap<>();
    private final Map<String, Double> alertThresholds = new HashMap<>();
    private final Map<String, XYChart.Series<Number, Number>> seriesMap = new HashMap<>();
    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
    private int refreshInterval = 20;
    private int timeIndex = 0;
    private Timeline timeline;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Real-Time Stock Tracker");

        // UI Elements
        TextField symbolsInput = new TextField();
        symbolsInput.setPromptText("Enter symbols: AAPL,GOOGL");

        TextField intervalInput = new TextField("20");
        intervalInput.setPromptText("Refresh (sec)");

        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);  // disabled by default

        HBox inputBox = new HBox(10, new Label("Symbols:"), symbolsInput, new Label("Interval:"), intervalInput, startButton, stopButton);
        inputBox.setPadding(new Insets(10));

        // Table setup
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                createColumn("Symbol", "symbol"),
                createColumn("Price (USD)", "price"),
                createColumn("% Change", "change"),
                createColumn("Alert", "alertStatus")
        );

        // Chart setup
        lineChart.setTitle("Stock Price History");
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(true);

        VBox root = new VBox(10, inputBox, table, lineChart);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();

        // Button actions
        startButton.setOnAction(e -> {
            String[] symbols = symbolsInput.getText().split(",");
            try {
                refreshInterval = Integer.parseInt(intervalInput.getText());
            } catch (NumberFormatException ex) {
                refreshInterval = 20;
            }

            setupAlertDialog(symbols);
            startAutoRefresh(symbols);
            startButton.setDisable(true);
            stopButton.setDisable(false);
        });

        stopButton.setOnAction(e -> {
            if (timeline != null) {
                timeline.stop();
            }
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
    }

    private void setupAlertDialog(String[] symbols) {
        for (String rawSymbol : symbols) {
            String symbol = rawSymbol.trim().toUpperCase();
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Price Alert");
            dialog.setHeaderText("Set Alert for " + symbol);
            dialog.setContentText("Alert if price crosses (leave blank to skip):");

            dialog.showAndWait().ifPresent(input -> {
                try {
                    if (!input.isBlank()) {
                        alertThresholds.put(symbol, Double.parseDouble(input));
                    }
                } catch (NumberFormatException ignored) {}
            });
        }
    }

    private void startAutoRefresh(String[] symbols) {
        timeIndex = 0;
        lastPrices.clear();
        seriesMap.clear();
        lineChart.getData().clear();

        timeline = new Timeline(new KeyFrame(Duration.seconds(refreshInterval), e -> {
            timeIndex++;
            List<StockData> updated = new ArrayList<>();

            for (String raw : symbols) {
                String symbol = raw.trim().toUpperCase();
                try {
                    double price = fetchPrice(symbol);
                    double last = lastPrices.getOrDefault(symbol, -1.0);
                    lastPrices.put(symbol, price);

                    double changePercent = (last > 0) ? ((price - last) / last) * 100 : 0;
                    String changeStr = (last > 0) ? String.format("%.2f%%", changePercent) : "-";
                    String alertStr = alertThresholds.containsKey(symbol) && price >= alertThresholds.get(symbol)
                            ? "🔔 Triggered!" : "-";

                    updated.add(new StockData(symbol, String.format("%.2f", price), changeStr, alertStr));

                    XYChart.Series<Number, Number> series = seriesMap.computeIfAbsent(symbol, key -> {
                        XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
                        newSeries.setName(key);
                        lineChart.getData().add(newSeries);
                        return newSeries;
                    });

                    series.getData().add(new XYChart.Data<>(timeIndex, price));

                } catch (Exception ex) {
                    updated.add(new StockData(symbol, "Error", "-", "-"));
                }
            }

            table.getItems().setAll(updated);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private double fetchPrice(String symbol) throws Exception {
        String url = "https://api.twelvedata.com/price?symbol=" + symbol + "&apikey=" + ConfigLoader.getApiKey();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        if (json.has("price")) return Double.parseDouble(json.get("price").getAsString());
        throw new Exception(json.has("message") ? json.get("message").getAsString() : "Unknown error");
    }

    private TableColumn<StockData, String> createColumn(String title, String property) {
        TableColumn<StockData, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> cellData.getValue().getProperty(property));
        return col;
    }
}
