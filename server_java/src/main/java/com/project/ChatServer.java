package com.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

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
    private volatile boolean running = true;


    public ChatServer (int port) {
        super(new InetSocketAddress(port));
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
        String cd="cd";
        String workingdirectory = "~/dev/rpi-rgb-led-matrix";
        String prueba= "examples-api-use/demo -D0 --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse";
        executeCommand(prueba,workingdirectory);
        Thread userInputThread = new Thread(this::handleUserInput);
        userInputThread.start();
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
            String platform = objRequest.getString("platform");
            if (platform.equalsIgnoreCase("android")){
                String mensaje = objRequest.getString("message");
                System.out.println(mensaje+", Mensage enviado desde Android");
            }else if(platform.equalsIgnoreCase("flutter")){
                String mensaje = objRequest.getString("message");
                System.out.println(mensaje+", Mensage enviado desde Flutter");
            } else if (platform.equalsIgnoreCase("list")) {
                // El client demana la llista de tots els clients
                System.out.println("Client '" + clientId + "'' requests list of clients");
                sendList(conn);

            } else if (platform.equalsIgnoreCase("private")) {
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
                
            } else if (platform.equalsIgnoreCase("broadcast")) {
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

    public void runServerBucle() {
        boolean running = true;
    
        // Usar BufferedReader para leer la entrada del usuario
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    
        try {
            System.out.println("Starting server");
            start();
    
            while (running) {
                // Leer la línea de la entrada del usuario
                String line = reader.readLine();
    
                if (line.equalsIgnoreCase("exit")) {
                    running = false;
                }
            }
    
            System.out.println("Stopping server");
            stop(1000);
    
            // Cerrar el BufferedReader
            reader.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
private void executeCommand(String command, String workingDirectory) {
    try {
        // Reemplazar manualmente la expansión del directorio de inicio (~)
        if (workingDirectory.startsWith("~" + File.separator)) {
            workingDirectory = System.getProperty("user.home") + workingDirectory.substring(1);
        }

        // Construir el proceso usando ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.directory(new File(workingDirectory));
        processBuilder.redirectErrorStream(true);

        // Iniciar el proceso
        Process process = processBuilder.start();

        // Esperar a que termine el proceso
        process.waitFor();

        // Comprobar el resultado de la ejecución
        System.out.println("Command exit code: " + process.exitValue());

        // Leer y enviar la salida del comando al cliente (opcional)
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Enviar la salida del comando al cliente
        JSONObject objResponse = new JSONObject("{}");
        objResponse.put("type", "command_output");
        objResponse.put("from", "server");
        objResponse.put("value", output.toString());
        broadcast(objResponse.toString());

    } catch (Exception e) {
        e.printStackTrace();
    }
}
private void handleUserInput() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
        while (running) {
            String line = reader.readLine();
            if ("exit".equalsIgnoreCase(line)) {
                running = false;
                break;
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}


public void setOnClientConnectedListener(Object object) {
}
}
