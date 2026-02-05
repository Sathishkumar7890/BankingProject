package com.HostJar.LoadValues;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.stereotype.Component;



@Component
public class Load {
	
    private static final Logger logger = LogManager.getLogger(Load.class);
    private static final Path propertiesPath =
            Paths.get("D:/IVR/BankingApplication/config/banking_config.properties");
    private volatile long propertiesLastModified = -1;
    private volatile Map<String, String> propertiesMap = new HashMap<>();
    private static Properties properties = new Properties();
    private static long lastModified = 0;
    private static CloseableHttpClient Client ;
    
    private static final Load INSTANCE = new Load();
    
    public static CloseableHttpClient getClient() {
		return Client;
	}

	public static void setClient(CloseableHttpClient client) {
		Client = client;
	}

	public static JSONObject loadJson() {
        try {
           	 String Path = CONFIG.getProperty("MENU_DETAILS");
           	 
            byte[] bytes = Files.readAllBytes(Paths.get(Path));
            String jsonContent = new String(bytes, StandardCharsets.UTF_8);
 
            JSONObject rootJson = new JSONObject(jsonContent);
 
            System.out.println(rootJson);
            logger.info("Menu JSON loaded successfully");
            logger.debug("Complete Menu JSON:\n{}", rootJson.toString(4));
 
            return rootJson;
        } catch (Exception e) {
            logger.error("Error while loading json", e);
        }
        return null;
    }

	
	public Load() { }

    public static Load getInstance() {
        return INSTANCE;
    }

    // 🔹 Public getter (auto reload)
    public Map<String, String> getProperties() {
        loadProperties();
        return propertiesMap;
    }

	
	public void loadProperties() {

        try {

            
            if (!Files.exists(propertiesPath)) {
                throw new RuntimeException("CONFIG FILE NOT FOUND: " + propertiesPath);
            }

            long currentModified =
                    Files.getLastModifiedTime(propertiesPath).toMillis();

            // No change → skip reload
            if (currentModified == propertiesLastModified) {
                return;
            }

            synchronized (this) {

                // Double check
                if (currentModified == propertiesLastModified) {
                    return;
                }

                Properties tempProps = new Properties();

                try (InputStream in = Files.newInputStream(propertiesPath)) {
                    tempProps.load(in);
                }

                Map<String, String> tempMap = new HashMap<>();

                for (String key : tempProps.stringPropertyNames()) {
                    tempMap.put(key, tempProps.getProperty(key));
                }

                // Atomic swap
                propertiesMap = tempMap;
                propertiesLastModified = currentModified;

                logger.info("🔥 PROPERTIES RELOADED at {}", currentModified);
            }

        } catch (Exception e) {

            logger.error("CONFIG LOAD FAILED", e);

            
            e.printStackTrace();
        }
    }

	
    // Store all config key-values in a static Properties object
    public static final Properties CONFIG = new Properties();

    // Static block runs once when the class is loaded
    static {
        try (InputStream input = new FileInputStream("D:/Banking_Project/Config/Config.properties")) {
            CONFIG.load(input);
            logger.info("Configuration file loaded successfully");
            CONFIG.forEach((key, value) -> logger.debug("{} = {}", key, value));
        } catch (Exception e) {
            logger.error("Error while loading configuration file", e);
        }
    }
    
    public static void main(String[] args) {
   	  	Load.loadJson();
   	 
   	 System.out.println(CONFIG.getProperty("MENU_DETAILS"));
	}
}



