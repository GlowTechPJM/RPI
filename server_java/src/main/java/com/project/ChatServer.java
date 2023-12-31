package com.project;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import com.pi4j.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Processor;

// Make sure this import is correct
import java.io.*;
import java.util.Base64;


public class ChatServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private volatile boolean running = true;
    String firstprocces;
    int app = 0;
    int desktop = 0;
    static Process proceso;
    List<String> movil = new ArrayList<>();
    List<String> desk = new ArrayList<>();
    String wifiIP = metodos.getWifiIP();


    public ChatServer (int port) {
        super(new InetSocketAddress(port));
        
    }
    @Override
    public void onStart() {
        // Quan el servidor s'inicia

       
        int port = getAddress().getPort();
        proceso=executeDisplayCommandtexto(wifiIP);

        System.out.println("WebSockets server running at: ws://" + wifiIP + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);

    }
        
    

    

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // eliminamos la primera comanda
        // Quan un client es connecta
        String clientId = getConnectionId(conn);
        
        // Mostrem per pantalla (servidor) la nova connexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New client (" + clientId + "): " + host);
    }
    

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Quan un client es desconnecta
        String clientId = getConnectionId(conn);
        if (proceso != null){
            proceso.destroy();
            Iterator<String> movilIterator = movil.iterator();
            while (movilIterator.hasNext()) {
                String h = movilIterator.next();
                if (clientId.equals(h)) {
                    movilIterator.remove();
                    proceso = executeDisplayCommandtexto("conexion app: " + movil.size() + " conexion desktop: " + desk.size());
                }
                
            }
            Iterator<String> deskIterator = desk.iterator();
            while (deskIterator.hasNext()) {
                String h = deskIterator.next();
                if (clientId.equals(h)) {
                    deskIterator.remove();
                    proceso = executeDisplayCommandtexto("conexion app: " + movil.size() + " conexion desktop: " + desk.size());
                }
            
            }
            if(movil.size()==0 && desk.size()==0){
                    proceso.destroy();
                    proceso=executeDisplayCommandtexto(wifiIP);

                }
            System.out.println("Client disconnected '" + clientId + "'");
            JSONObject broadcas = new JSONObject("{}");
            broadcas.put("type", "broadcast");
            broadcas.put("action", "disconnect");
            broadcas.put("user", clientId);
            broadcast(broadcas.toString());
        }
        
        
        // Mostrem per pantalla (servidor) la desconnexió
        
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Quan arriba un missatge
        String clientId = getConnectionId(conn);

        HashMap<String, String> usuarios = metodos.getUsers();
        try {
            JSONObject objRequest = new JSONObject(message);
            if (objRequest.has("platform")) {
                String platform=objRequest.getString("platform");
                // Realiza acciones basadas en la plataforma del cliente
                if (platform.equalsIgnoreCase("android")) {
                    // Cliente conectado desde una aplicación Android
                    if (proceso != null){
                        movil.add(clientId);
                        proceso.destroy();
                        proceso =executeDisplayCommandtexto("conexion app: "+movil.size()+" conexion desktop: "+desk.size());
                    }
                    
                } else if (platform.equalsIgnoreCase("desktop")) {
                    // Cliente conectado desde un cliente de escritorio
                    if (proceso != null){
                        desk.add(clientId);
                        proceso.destroy();
                        proceso=executeDisplayCommandtexto("conexion app: "+movil.size()+" conexion desktop: "+desk.size());
                    }
                }
            } 
            if (objRequest.has("user")){
                String platform=objRequest.getString("userPlatform");
                System.err.println("intento de inicio de sesion");
                String usuario = objRequest.getString("user");
                if (usuarios.containsKey(usuario)){
                    String toocheck = usuarios.get(usuario);
                    String password = objRequest.getString("password");

                    if (password.equals(toocheck)){
                        JSONObject objResponse = new JSONObject("{}");
                        objResponse.put("validacion", "correcto");
                        String jsonString = objResponse.toString();
                        System.out.println(jsonString);
                        conn.send(jsonString);
                        
                        JSONObject broadcas = new JSONObject("{}");
                        broadcas.put("type", "broadcast");
                        broadcas.put("action", "connect");
                        broadcas.put("user", clientId);
                        broadcast(broadcas.toString());
                    }else{
                        JSONObject objResponse = new JSONObject("{}");
                        objResponse.put("validacion", "incorrecto");
                        String jsonString = objResponse.toString();
                        System.out.println(jsonString);
                        conn.send(jsonString);
                    }
                    
                }else{
                    if (platform.equalsIgnoreCase("android")) {
                        for (String i : movil){
                            if(i.equals(clientId)){
                                movil.remove(i);
                                proceso =executeDisplayCommandtexto("conexion app: "+movil.size()+" conexion desktop: "+desk.size());
                            }
                        }
                    }else if (platform.equalsIgnoreCase("desktop")){
                        for (String i : desk){
                            if(i.equals(clientId)){
                                desk.remove(i);
                                proceso =executeDisplayCommandtexto("conexion app: "+movil.size()+" conexion desktop: "+desk.size());
                            }
                        }
                    }
                    JSONObject objResponse = new JSONObject("{}");
                    objResponse.put("validacion", "incorrecto");
                    String jsonString = objResponse.toString();
                    System.out.println(jsonString);
                    conn.send(jsonString);
                }
            }
            if (objRequest.has("message")){
                String platform=objRequest.getString("msgPlatform");
                if (platform.equalsIgnoreCase("android")) {
                    for (String j : movil){
                        if (j.equals(clientId)){
                            String mensaje = objRequest.getString("message");
                            System.err.println("entra mensaje");
                            System.out.println();
                            if (proceso != null){
                                proceso.destroy();
                                proceso = executeDisplayCommandtexto(mensaje);
                            }
                        }
                    }
                    JSONObject broadcas = new JSONObject("{}");
                        broadcas.put("type", "broadcast");
                        broadcas.put("action", "message");
                        broadcas.put("user", clientId);
                        broadcast(broadcas.toString());  
                }else if (platform.equalsIgnoreCase("desktop")){
                    for (String j : desk){
                        if (j.equals(clientId)){
                            String mensaje = objRequest.getString("message");
                            System.err.println("entra mensaje");
                            System.out.println();
                            if (proceso != null){
                                proceso.destroy();
                                proceso = executeDisplayCommandtexto(mensaje);
                            }
                        }
                    }
                    JSONObject broadcas = new JSONObject("{}");
                        broadcas.put("type", "broadcast");
                        broadcas.put("action", "message");
                        broadcas.put("user", clientId);
                        broadcast(broadcas.toString());  
                }else{
                        System.out.println("no estas connectado no valida");
                }
                 
            }
            if(objRequest.has("imagen")){
                String pathimg = "/home/ieti/Desktop/RPI/server_java/data/imagen.png" ;
                String image = objRequest.getString("imagen");
                if (objRequest.has("imgPlatform")){
                    String plt = objRequest.getString("imgPlatform");
                    if (plt.equals("android")){
                        for(String f : movil){
                            if (f.equals(clientId)){
                                System.out.println(image);
                                convertirImagen(image, pathimg);
                                if (proceso != null){
                                    proceso.destroy();
                                    proceso = executeDisplayCommandimage(pathimg);
                                }

                            }
                        }
                        JSONObject broadcas = new JSONObject("{}");
                        broadcas.put("type", "broadcast");
                        broadcas.put("action", "message");
                        broadcas.put("user", clientId);
                        broadcast(broadcas.toString());  
                    }else if(plt.equals("desktop")){
                        for(String f : desk){
                            if (f.equals(clientId)){
                                System.out.println(image);
                                convertirImagen(image, pathimg);
                                if (proceso != null){
                                    proceso.destroy();
                                    proceso = executeDisplayCommandimage(pathimg);
                                }
                            }
                        }
                        JSONObject broadcas = new JSONObject("{}");
                        broadcas.put("type", "broadcast");
                        broadcas.put("action", "message");
                        broadcas.put("user", clientId);
                        broadcast(broadcas.toString());  
                    }else{
                        System.out.println("no estas connectado no valida");
                    }
                }
            };     //a     
            if (objRequest.has("connected")){
                if (objRequest.has("cntPlatform")){ 
                    String listaMovil = String.join(";", movil);
                    String listaDesk = String.join(";", desk);               
                    String platform = objRequest.getString("cntPlatform");
                    if (platform.equalsIgnoreCase("android")){
                        for(String f : movil){
                            if (f.equals(clientId)){
                                JSONObject listas = new JSONObject("{}");
                                listas.put("action","listas");
                                listas.put("android", listaMovil);
                                listas.put("desk", listaDesk);
                                conn.send(listas.toString());
                            }
                        }
                    }else if(platform.equalsIgnoreCase("desktop")){
                        for(String f : desk){
                            if (f.equals(clientId)){
                                JSONObject listas = new JSONObject("{}");
                                listas.put("android", listaMovil);
                                listas.put("desk", listaDesk);
                                conn.send(listas.toString());
                            }
                        }
                    }
                }
            };
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
                    executeKillCommand(getFirstProcess());
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

    public static Process executeDisplayCommandtexto(String text) {
        Process proceso = null;
        try {
            String command = "cd ~/dev/rpi-rgb-led-matrix && examples-api-use/scrolling-text-example -x 100 -y 10 -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=54 --led-slowdown-gpio=4 --led-no-hardware-pulse " + text;
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            proceso = processBuilder.start();
            System.out.println("estoy printando");
            InputStream inputStream = proceso.getInputStream();
            OutputStream outputStream = proceso.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proceso;
    }
    public static Process executeDisplayCommandimage(String image) {
        try {
            String command = "cd ~/dev/rpi-rgb-led-matrix && led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse "+image;
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            proceso = processBuilder.start();

            InputStream inputStream = proceso.getInputStream();
            OutputStream outputStream = proceso.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);
            writer.println(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return proceso;
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
    public static void convertirImagen(String base64String, String imagePath) {
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64String);

            OutputStream outputStream = new FileOutputStream(imagePath);
            outputStream.write(imageBytes);
            outputStream.close();

            System.out.println("Imagen decodificada y guardada como " + imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


public void setOnClientConnectedListener(Object object) {
}
}


