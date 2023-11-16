package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.io.OutputStream;  // Make sure this import is correct


import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;







public class ChatServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private volatile boolean running = true;
    String firstprocces;

    public ChatServer (int port) {
        super(new InetSocketAddress(port));
    }
    @Override
    public void onStart() {
        // Quan el servidor s'inicia
        String wifiIP = metodos.getWifiIP();
        int port = getAddress().getPort();
        System.out.println("WebSockets server running at: ws://" + wifiIP + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);

        executeDisplayCommand(wifiIP);;
    }
        
    

    

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        
        // Quan un client es connecta
        String clientId = getConnectionId(conn);

        // matem el primer process
        executeKillCommand(getFirstProcess());
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

            // Esperar a que todas las conexiones se cierren antes de detener el servidor
            System.out.println("Waiting for connections to close...");
            while (getConnections().size() > 0) {
                TimeUnit.MILLISECONDS.sleep(100);
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

    public static void executeDisplayCommand(String text) {
        try {
            String command = "cd ~/dev/rpi-rgb-led-matrix && ./text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse "+text;
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            Process proceso = processBuilder.start();

            InputStream inputStream = proceso.getInputStream();
            OutputStream outputStream = proceso.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     public static String getFirstProcess() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ps aux | grep text");
            Process proceso = processBuilder.start();
            InputStream inputStream = proceso.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Pattern pattern = Pattern.compile("^\\s*(\\d+).*");
            StringBuilder outputBuilder = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                outputBuilder.append(linea).append("\n");
                Matcher matcher = pattern.matcher(linea);
                if (matcher.matches()) {
                    String numeroProceso = matcher.group(1);
                    break;
                }
            }
            int resultado = proceso.waitFor();
            return outputBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
    public static void executeKillCommand(String numeroProceso) {
        try {
            String killCommand = "kill " + numeroProceso;
            ProcessBuilder killProcessBuilder = new ProcessBuilder("bash", "-c", killCommand);
            Process killProceso = killProcessBuilder.start();
            int resultadoKill = killProceso.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    


public void setOnClientConnectedListener(Object object) {
}
}


