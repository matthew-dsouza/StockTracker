package com.example;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StockData {
    private final StringProperty symbol;
    private final StringProperty price;
    private final StringProperty change;
    private final StringProperty alertStatus;

    public StockData(String symbol, String price, String change, String alertStatus) {
        this.symbol = new SimpleStringProperty(symbol);
        this.price = new SimpleStringProperty(price);
        this.change = new SimpleStringProperty(change);
        this.alertStatus = new SimpleStringProperty(alertStatus);
    }

    public StringProperty getProperty(String name) {
        return switch (name) {
            case "symbol" -> symbol;
            case "price" -> price;
            case "change" -> change;
            case "alertStatus" -> alertStatus;
            default -> new SimpleStringProperty("-");
        };
    }
}
