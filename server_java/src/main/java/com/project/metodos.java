package com.project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

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

public static void ejecutarComandoEnDirectorio(String directorio, String comando) {
    try {
        // Crear el proceso builder con el comando y el directorio de trabajo
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", comando);
        processBuilder.directory(new java.io.File(directorio));

        // Iniciar el proceso
        Process proceso = processBuilder.start();

        // Esperar a que el proceso termine
        int codigoSalida = proceso.waitFor();

        // Imprimir la salida del proceso
        java.util.Scanner s = new java.util.Scanner(proceso.getInputStream()).useDelimiter("\\A");
        String salida = s.hasNext() ? s.next() : "";

        // Imprimir el resultado
        System.out.println("Resultado del comando '" + comando + "':\n" + salida);
        System.out.println("Código de salida: " + codigoSalida);

    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
}
    
}
