package functionality;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_PATH = "config/app_config.properties";
    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load app configuration: " + CONFIG_PATH);
            System.err.println("Reason: " + e.getMessage());
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLongProperty(String key, long defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Accessors
    public static String getServerHost() {
        return getProperty("server.host", "127.0.0.1");
    }

    public static int getServerPort() {
        return getIntProperty("server.port", 9999);
    }

    public static long getTimestampWindowMs() {
        return getLongProperty("auth.timestamp.window.ms", 30000L);
    }

    public static String getUsersFilePath() {
        return getProperty("path.users", "data/users.txt");
    }

    public static String getHistoryFilePath() {
        return getProperty("path.history", "data/login_history.txt");
    }

    public static String getAuthFilePath() {
        return getProperty("path.auth", "vpn-auth.txt");
    }

    public static String getConfigDir() {
        return getProperty("config.dir", "openvpn-configs");
    }
}
