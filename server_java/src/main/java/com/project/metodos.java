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
    public static void ejecutarComanda() {
        String cmd[] = {"bash", "-c","cd","~/dev/rpi-rgb-led-matrix"};
        String ls[] ={"bash", "-c","ls"};
        try {
            // objecte global Runtime
            Runtime rt = java.lang.Runtime.getRuntime();
 
            // executar comanda en subprocess
            Process p = rt.exec(cmd);
            // donem un temps d'execució
            Process i = rt.exec(ls);
            TimeUnit.SECONDS.sleep(5);
            // el matem si encara no ha acabat
            if( p.isAlive() ) p.destroy();
            p.waitFor();
            i.waitFor();
            // comprovem el resultat de l'execució
            System.out.println("Comanda 1 exit code="+p.exitValue());
 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
