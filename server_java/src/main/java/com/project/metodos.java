package com.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.java_websocket.util.Base64.OutputStream;

public class metodos {
    public static String getWifiIP() {
    try {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                    return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
        e.printStackTrace();
    }
    return "No se encontró una dirección IP de WiFi.";
    }
    public static HashMap<String, String> getUsers(){
        HashMap<String, String> usuarios = new HashMap<>();

        // Agregar usuarios y contraseñas al HashMap
        usuarios.put("usuario1", "contra1");
        usuarios.put("usuario2", "contra2");
        usuarios.put("usuario3", "contra3");
        return usuarios; 
    }
}


