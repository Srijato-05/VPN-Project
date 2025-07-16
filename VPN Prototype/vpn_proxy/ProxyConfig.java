package vpn_proxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ProxyConfig {
    private static final String CONFIG_PATH = "config/proxy.properties";
    private static final Properties props = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load proxy configuration: " + CONFIG_PATH);
            System.err.println("Reason: " + e.getMessage());
        }
    }

    public static int getListenPort() {
        try {
            return Integer.parseInt(props.getProperty("listenPort", "8888"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid listenPort value. Using default 8888.");
            return 8888;
        }
    }

    public static String getForwardHost() {
        return props.getProperty("forwardHost", "localhost");
    }

    public static int getForwardPort() {
        try {
            return Integer.parseInt(props.getProperty("forwardPort", "9999"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid forwardPort value. Using default 9999.");
            return 9999;
        }
    }
}
