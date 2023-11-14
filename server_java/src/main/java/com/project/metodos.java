package com.project;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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
    public static void ejecutarComanda() {
        System.out.println("Iniciando comandos...");

        // Directorio deseado
        String directorio = "~/dev/rpi-rgb-led-matrix";

        // Comando para cambiar de directorio
        String cambiarDirectorioCmd = "cd " + directorio;

        // Comando "ls" para listar archivos
        String listarArchivosCmd = "ls";

        try {
            // Ejecutar el comando para cambiar de directorio
            Process cambiarDirProcess = Runtime.getRuntime().exec(new String[] {"bash", "-c", cambiarDirectorioCmd});
            cambiarDirProcess.waitFor();

            // Ejecutar el comando "ls" en el nuevo directorio
            Process lsProcess = Runtime.getRuntime().exec(new String[] {"bash", "-c", listarArchivosCmd});
            lsProcess.waitFor();

            // Imprimir la salida del comando "ls"
            java.util.Scanner s = new java.util.Scanner(lsProcess.getInputStream()).useDelimiter("\\A");
            String output = s.hasNext() ? s.next() : "";
            System.out.println("Resultado de 'ls':\n" + output);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Comandos finalizados.");
    }
    
}
