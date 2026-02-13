package com.HostJar.LoadValues;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;


public class Load {

	
	 DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
             .withZone(ZoneId.systemDefault());
	
	  private final Properties config = new Properties();
    private volatile long configLastModified = -1;
    private volatile long configReloadTime = -1; // Stores when config was reloaded

    // JSON tracking
    private volatile long menuJsonLastModified = -1;
    private volatile long jsonReloadTime = -1;   
    private volatile JSONObject menuJson;

    // HTTP client
    private static CloseableHttpClient client;

    // Singleton instance
    private static final Load INSTANCE = new Load();

    // Shared Properties object
    public static final Properties CONFIG = new Properties();

  
    public static CloseableHttpClient getClient() {
        return client;
    }

    public static void setClient(CloseableHttpClient httpClient) {
        client = httpClient;
    }

    
    private Load() {}

    public static Load getInstance() {
        return INSTANCE;
    }

    // =====================================================
    // ✅ LOAD CONFIG (AUTO RELOAD WHEN FILE CHANGES)
    // =====================================================

    public synchronized void loadConfig(String configFilePath) {

        try {

            Path path = Paths.get(configFilePath);

            if (!Files.exists(path)) {
                throw new RuntimeException("CONFIG FILE NOT FOUND: " + path);
            }

            long currentModified =
                    Files.getLastModifiedTime(path).toMillis();

            // ✅ Skip reload if file not changed
            if (currentModified == configLastModified) {
                return;
            }

            try (InputStream in = new FileInputStream(configFilePath)) {

                config.clear();
                config.load(in);
            }

            configLastModified = currentModified;

            System.out.println("✅ CONFIG RELOADED at file time: "
                    + timeFormatter.format(
                    Instant.ofEpochMilli(configLastModified)));

            // 🔥 Debug once (remove later if needed)
            System.out.println("Loaded Keys -> " + config.keySet());

        } catch (Exception e) {

            throw new RuntimeException("CONFIG LOAD FAILED", e);
        }
    }

    // =====================================================
    // ✅ SAFE PROPERTY GETTERS
    // =====================================================

    public String getProperty(String key) {

        String value = config.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Missing config key: " + key);
        }

        return value.trim();
    }

    // ⭐ Recommended getter (prevents IVR crash)
    public String getProperty(String key, String defaultValue) {

        String value = config.getProperty(key);

        return (value == null || value.trim().isEmpty())
                ? defaultValue
                : value.trim();
    }

    public long getLong(String key, long defaultValue) {

        try {
            return Long.parseLong(
                    config.getProperty(key, String.valueOf(defaultValue)));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    // =====================================================
    // ✅ LOAD MENU JSON (AUTO RELOAD)
    // =====================================================

    public JSONObject loadMenuJson() {

        try {

            String jsonPathStr = getProperty("MENU_DETAILS");

            Path jsonPath = Paths.get(jsonPathStr);

            if (!Files.exists(jsonPath)) {
                throw new RuntimeException(
                        "Menu JSON file NOT FOUND: " + jsonPathStr);
            }

            long currentModified =
                    Files.getLastModifiedTime(jsonPath).toMillis();

            if (menuJson != null &&
                    currentModified == menuJsonLastModified) {

                return menuJson;
            }

            synchronized (this) {

                if (menuJson != null &&
                        currentModified == menuJsonLastModified) {

                    return menuJson;
                }

                byte[] bytes = Files.readAllBytes(jsonPath);

                String jsonContent =
                        new String(bytes, StandardCharsets.UTF_8);

                menuJson = new JSONObject(jsonContent);

                menuJsonLastModified = currentModified;

                System.out.println("✅ MENU JSON RELOADED at file time: "
                        + timeFormatter.format(
                        Instant.ofEpochMilli(menuJsonLastModified)));
            }

            return menuJson;

        } catch (Exception e) {

            throw new RuntimeException("FAILED TO LOAD MENU JSON", e);
        }
    }

    
 



    public static void init(String configPath) {

        Load loader = getInstance();

        loader.loadConfig(configPath);

        System.out.println("🚀 APPLICATION CONFIG INITIALIZED");
    }


    public static void main(String[] args) {

        String configPath =
                "D:/Banking_Project/Config/Config.properties";

        Load.init(configPath);

        Load loader = Load.getInstance();

        String menuPath = loader.getProperty("MENU_DETAILS");

        System.out.println("MENU_DETAILS -> " + menuPath);

        JSONObject menu = loader.loadMenuJson();

        System.out.println(menu.toString(4));
    }
}

    



