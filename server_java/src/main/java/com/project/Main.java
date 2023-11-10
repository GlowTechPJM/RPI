package com.project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class Main extends Application {
    private static final int WEBSOCKET_PORT = 8080;
    @Override
    public void start(Stage primaryStage) {
        ChatServer webSocketServer = new ChatServer(WEBSOCKET_PORT);
        webSocketServer.start();

        // Crear una etiqueta para mostrar la dirección IP
        Label ipAddressLabel = new Label("Dirección IP de la WiFi: " + getWiFiIPAddress());

        // Crear un VBox y agregar la etiqueta
        VBox root = new VBox(ipAddressLabel);

        // Crear una escena y agregarla al escenario
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Raspberry Pi Display App");
        primaryStage.show();
        clearDisplayIPAddress();

    }

    private void clearDisplayIPAddress() {

    }

    // Método para obtener la dirección IP de la WiFi
    private String getWiFiIPAddress() {
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

    public static void main(String[] args) {
        launch(args);
    }
}
