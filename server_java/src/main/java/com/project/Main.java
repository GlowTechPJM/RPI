package com.project;

import java.util.Enumeration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class Main {
    public static void main (String[] args) throws InterruptedException, IOException {

        int port = 8080; 
        String wifi = getWiFiIPAddress();
        System.out.println("Dirección IP WiFi: " + wifi);

        // Deshabilitar SSLv3 per clients Android
        java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        ChatServer server = new ChatServer(port);
        server.runServerBucle();
    }

    // Método para obtener la dirección IP de la WiFi
    private static String getWiFiIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "No se pudo encontrar la dirección IP WiFi";
    }

    
}
