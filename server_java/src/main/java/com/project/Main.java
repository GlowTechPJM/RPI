package com.project;

import java.util.Enumeration;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main (String[] args) throws InterruptedException, IOException {

        int port = 8080; 
        String wifi = getWiFiIPAddress();
        System.out.println("Dirección IP WiFi: " + wifi);

        // Deshabilitar SSLv3 per clients Android
        java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        Pin[] ledPinNumbers={RaspiPin.GPIO_00, RaspiPin.GPIO_01, RaspiPin.GPIO_02, RaspiPin.GPIO_03};
        ChatServer server = new ChatServer(port,ledPinNumbers);
        server.runServerBucle();
    }

    // Método para obtener la dirección IP de la WiFi
    public static String getWiFiIPAddress() throws SocketException, UnknownHostException {
        String wifiIp = "";
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (ni.getName().toLowerCase().contains("wlan")) { // Filtra interfaces WiFi
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ia = inetAddresses.nextElement();
                    if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia.isSiteLocalAddress()) {
                        System.out.println(ni.getDisplayName() + ": " + ia.getHostAddress());
                        wifiIp = ia.getHostAddress();
                        // Si hay múltiples direcciones IP, se queda con la última
                    }
                }
            }
        }

        // Si no encuentra ninguna dirección IP WiFi, retorna la loopback
        if (wifiIp.isEmpty()) {
            wifiIp = InetAddress.getLocalHost().getHostAddress();
        }
        return wifiIp;
    }

    
}
