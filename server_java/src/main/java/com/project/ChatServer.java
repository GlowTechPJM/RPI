package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;


public class ChatServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private static GpioPinDigitalOutput[] ledPins;

    public ChatServer (int port, Pin[] ledPinNumbers) {
        super(new InetSocketAddress(port));
        // Inicializa los pines GPIO para los LEDs
        GpioController gpioController = GpioFactory.getInstance();
        ledPins = new GpioPinDigitalOutput[ledPinNumbers.length];

        for (int i = 0; i < ledPinNumbers.length; i++) {
            ledPins[i] = gpioController.provisionDigitalOutputPin(ledPinNumbers[i], "LED " + i, PinState.LOW);
            ledPins[i].setShutdownOptions(true, PinState.LOW);
        }
    }
    @Override
    public void onStart() {
        // Quan el servidor s'inicia
        String wifiIP = getWifiIP();
        int port = getAddress().getPort();
        System.out.println("WebSockets server running at: ws://" + wifiIP + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);;
        displayIPAddress(wifiIP);
    }

    

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        
        // Quan un client es connecta
        String clientId = getConnectionId(conn);

        // Saludem personalment al nou client
        JSONObject objWlc = new JSONObject("{}");
        objWlc.put("type", "private");
        objWlc.put("from", "server");
        objWlc.put("value", "Bienvenido a la aplicación");
        conn.send(objWlc.toString()); 

        // Li enviem el seu identificador
        JSONObject objId = new JSONObject("{}");
        objId.put("type", "id");
        objId.put("from", "server");
        objId.put("value", clientId);
        conn.send(objId.toString()); 

        // Enviem al client la llista amb tots els clients connectats
        sendList(conn);

        // Enviem la direcció URI del nou client a tothom 
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "connected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la nova connexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New client (" + clientId + "): " + host);
    }
    

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Quan un client es desconnecta
        String clientId = getConnectionId(conn);

        // Informem a tothom que el client s'ha desconnectat
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "disconnected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la desconnexió
        System.out.println("Client disconnected '" + clientId + "'");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Quan arriba un missatge
        String clientId = getConnectionId(conn);
        try {
            JSONObject objRequest = new JSONObject(message);
            String type = objRequest.getString("type");

            if (type.equalsIgnoreCase("list")) {
                // El client demana la llista de tots els clients
                System.out.println("Client '" + clientId + "'' requests list of clients");
                sendList(conn);

            } else if (type.equalsIgnoreCase("private")) {
                // El client envia un missatge privat a un altre client
                System.out.println("Client '" + clientId + "'' sends a private message");

                JSONObject objResponse = new JSONObject("{}");
                objResponse.put("type", "private");
                objResponse.put("from", clientId);
                objResponse.put("value", objRequest.getString("value"));

                String destination = objRequest.getString("destination");
                WebSocket desti = getClientById(destination);

                if (desti != null) {
                    desti.send(objResponse.toString()); 
                }
                
            } else if (type.equalsIgnoreCase("broadcast")) {
                // El client envia un missatge a tots els clients
                System.out.println("Client '" + clientId + "'' sends a broadcast message to everyone");

                JSONObject objResponse = new JSONObject("{}");
                objResponse.put("type", "broadcast");
                objResponse.put("from", clientId);
                objResponse.put("value", objRequest.getString("value"));
                broadcast(objResponse.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // Quan hi ha un error
        ex.printStackTrace();
    }

    public void runServerBucle () {
        boolean running = true;
        try {
            System.out.println("Starting server");
            start();
            while (running) {
                String line;
                line = in.readLine();
                if (line.equals("exit")) {
                    running = false;
                }
            } 
            System.out.println("Stopping server");
            stop(1000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }  
    }

    private void displayIPAddress(String ipAddress) {
        // Muestra la dirección IP en la matriz de LEDs
        // Ajusta este código según la configuración de tu pantalla de LEDs

        // Por ejemplo, asumimos que cada dígito de la dirección IP se muestra en un LED diferente
        for (int i = 0; i < ipAddress.length(); i++) {
            char digit = ipAddress.charAt(i);
            int ledIndex = i; // Ajusta esto según la configuración de tu pantalla de LEDs

            if (digit == '.') {
                // Puedes ignorar el punto si estás mostrando cada dígito en un LED separado
                continue;
            }

            // Enciende o apaga el LED según el valor del dígito
            boolean isOn = digit != '0'; // Ejemplo: Enciende si el dígito no es '0'
            ledPins[ledIndex].setState(isOn);
        }
    }

    public void sendList (WebSocket conn) {
        JSONObject objResponse = new JSONObject("{}");
        objResponse.put("type", "list");
        objResponse.put("from", "server");
        objResponse.put("list", getClients());
        conn.send(objResponse.toString()); 
    }

    public String getConnectionId (WebSocket connection) {
        String name = connection.toString();
        return name.replaceAll("org.java_websocket.WebSocketImpl@", "").substring(0, 3);
    }

    public String[] getClients () {
        int length = getConnections().size();
        String[] clients = new String[length];
        int cnt = 0;

        for (WebSocket ws : getConnections()) {
            clients[cnt] = getConnectionId(ws);               
            cnt++;
        }
        return clients;
    }

    public WebSocket getClientById (String clientId) {
        for (WebSocket ws : getConnections()) {
            String wsId = getConnectionId(ws);
            if (clientId.compareTo(wsId) == 0) {
                return ws;
            }               
        }
        
        return null;
    }

private String getWifiIP() {
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

public void setOnClientConnectedListener(Object object) {
}
}