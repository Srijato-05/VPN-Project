import functionality.ClientGUI;
import functionality.Server;
import vpn_proxy.ProxyMain;

import javax.swing.*;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Launcher {

    public static void main(String[] args) {
        if (args.length > 0) {
            String component = args[0].toLowerCase();
            String[] remainingArgs = new String[args.length - 1];
            System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);

            switch (component) {
                case "server":
                    System.out.println("Launching Authentication Server...");
                    Server.main(remainingArgs);
                    return;
                case "proxy":
                    System.out.println("Launching VPN Proxy...");
                    ProxyMain.main(remainingArgs);
                    return;
                case "test":
                    System.out.println("Launching Comprehensive Test Suite...");
                    try {
                        VpnSystemTest.main(remainingArgs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                case "client":
                    launchClient(remainingArgs);
                    return;
                default:
                    System.out.println("Unknown component requested. Launching default Client dashboard...");
                    launchClient(args);
                    return;
            }
        } else {
            // Default action: launch client GUI
            launchClient(args);
        }
    }

    private static void launchClient(String[] args) {
        if (isWindows()) {
            if (isWindowsAdmin()) {
                System.out.println("Running with Administrator privileges. Launching Client GUI...");
                ClientGUI.main(args);
            } else {
                System.out.println("Client requires Administrator privileges to manage network routing and adapters.");
                System.out.println("Attempting secure UAC elevation...");
                try {
                    relaunchAsAdmin(args);
                } catch (Exception e) {
                    System.err.println("CRITICAL: Elevation request failed: " + e.getMessage());
                    e.printStackTrace();
                    showErrorDialog("Elevation Failed", "Could not restart as Administrator.\n\nPlease manually right-click the file and select 'Run as Administrator'.\n\nError: " + e.getMessage());
                    System.exit(1);
                }
            }
        } else {
            System.out.println("Non-Windows OS detected. Launching client GUI directly (requires root/sudo externally for OpenVPN).");
            ClientGUI.main(args);
        }
    }

    private static void showErrorDialog(String title, String message) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        JOptionPane.showMessageDialog(null, message, "Vanguard-VPN - " + title, JOptionPane.ERROR_MESSAGE);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isWindowsAdmin() {
        try {
            // net session returns exit code 0 if running as Administrator on Windows
            Process process = Runtime.getRuntime().exec("net session");
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void relaunchAsAdmin(String[] args) throws Exception {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            throw new Exception("JAVA_HOME is not set or accessible.");
        }

        File javaBinFile = new File(javaHome, "bin/java.exe");
        if (!javaBinFile.exists() || !javaBinFile.canExecute()) {
            throw new Exception("Java executable not found at: " + javaBinFile.getAbsolutePath());
        }
        String javaBin = javaBinFile.getAbsolutePath();

        String jarPath;
        try {
            jarPath = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new Exception("Failed to resolve JAR execution path.", e);
        }

        if (!jarPath.toLowerCase().endsWith(".jar")) {
            throw new Exception("Application must be packaged as a .jar file to auto-elevate.");
        }

        // Sanitize arguments to prevent PowerShell injection
        List<String> sanitizedArgs = new ArrayList<>();
        sanitizedArgs.add("-jar");
        sanitizedArgs.add(jarPath);
        sanitizedArgs.add("client");
        for (String arg : args) {
            if (arg != null && !arg.isEmpty() && arg.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
                sanitizedArgs.add(arg);
            }
        }

        // Construct safe PowerShell execution arguments using a dedicated ProcessBuilder
        // We use single quotes in PowerShell to denote literal strings, replacing internal single quotes to prevent breakouts.
        // We separate arguments with commas so PowerShell parses them as an array of arguments for -ArgumentList.
        StringBuilder argBuilder = new StringBuilder();
        argBuilder.append("'-Xmx256m', '-Xms64m'"); // Failsafe: Prevent memory exhaustion on infrastructure
        for (String arg : sanitizedArgs) {
            argBuilder.append(", '").append(arg.replace("'", "''")).append("'");
        }

        // Failsafe: When PowerShell elevates, it often defaults the working directory to C:\Windows\System32
        // We MUST force the working directory to the JAR's location so we don't accidentally write config files into critical infrastructure paths!
        File workingDirectory = new File(jarPath).getParentFile();
        if (workingDirectory == null || !workingDirectory.exists()) {
            throw new Exception("CRITICAL FAILSAFE: Cannot determine safe working directory.");
        }

        String psCommand = "Set-Location -LiteralPath '" + workingDirectory.getAbsolutePath().replace("'", "''") + "'; " +
                           "Start-Process -FilePath '" + javaBin.replace("'", "''") + "' " +
                           "-ArgumentList " + argBuilder.toString().trim() + " " +
                           "-Verb RunAs";

        System.out.println("Executing elevation payload...");
        
        // Resolve absolute path to powershell.exe to prevent CreateProcess error=2 when PATH is restricted
        // Checks Sysnative first to bypass 32-bit to 64-bit filesystem redirection on Windows x64
        String windir = System.getenv("SystemRoot");
        if (windir == null) windir = System.getenv("windir");
        if (windir == null) windir = "C:\\Windows";
        
        String[] possiblePsPaths = {
            "Sysnative\\WindowsPowerShell\\v1.0\\powershell.exe",
            "System32\\WindowsPowerShell\\v1.0\\powershell.exe",
            "SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe"
        };
        
        String psPath = "powershell.exe"; // fallback
        for (String relPath : possiblePsPaths) {
            File testFile = new File(windir, relPath);
            if (testFile.exists()) {
                psPath = testFile.getAbsolutePath();
                break;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(psPath, "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", psCommand);
        pb.directory(workingDirectory); // Failsafe: Bind local ProcessBuilder to safe directory
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new Exception("PowerShell elevation command returned exit code: " + exitCode);
        }
        
        System.exit(0);
    }
}
