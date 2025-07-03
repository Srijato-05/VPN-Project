import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class NetUtils {
    public static String sendHttpRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        boolean isHttps = url.getProtocol().equalsIgnoreCase("https");

        BufferedReader in;
        StringBuilder response = new StringBuilder();

        if (isHttps) {
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VPN-Client");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VPN-Client");
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        in.close();
        return response.toString();
    }
}
