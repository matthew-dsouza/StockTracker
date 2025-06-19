package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    public static String getApiKey() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
            return props.getProperty("API_KEY");
        } catch (IOException e) {
            System.err.println("Could not load API key: " + e.getMessage());
            return null;
        }
    }
}
