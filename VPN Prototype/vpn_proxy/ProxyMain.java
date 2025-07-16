package vpn_proxy;

import functionality.NetUtils;
import java.io.*;
import java.net.*;

public class ProxyMain {

    public static void main(String[] args) {
        int listenPort = ProxyConfig.getListenPort();
        String forwardHost = ProxyConfig.getForwardHost();
        int forwardPort = ProxyConfig.getForwardPort();

        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            NetUtils.logProxy("Proxy started on port " + listenPort +
                    ", forwarding to " + forwardHost + ":" + forwardPort);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    NetUtils.logProxy("Accepted client from " + clientSocket.getInetAddress());

                    new Thread(() -> handleClient(clientSocket, forwardHost, forwardPort)).start();

                } catch (IOException e) {
                    NetUtils.logProxy("Failed to accept connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            NetUtils.logProxy("Failed to start proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket, String forwardHost, int forwardPort) {
        try (
                Socket serverSocket = new Socket(forwardHost, forwardPort);
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream()
        ) {

            Thread c2s = new Thread(() -> {
                try {
                    pipeData(clientIn, serverOut);
                } catch (IOException e) {
                    NetUtils.logProxy("Error forwarding client → server: " + e.getMessage());
                }
            });

            Thread s2c = new Thread(() -> {
                try {
                    pipeData(serverIn, clientOut);
                } catch (IOException e) {
                    NetUtils.logProxy("Error forwarding server → client: " + e.getMessage());
                }
            });

            c2s.start();
            s2c.start();

            c2s.join();
            s2c.join();

        } catch (Exception e) {
            NetUtils.logProxy("Proxy error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void pipeData(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
    }
}
