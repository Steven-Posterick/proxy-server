package Proxy;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.regex.Matcher;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {


    Socket clientSocket;

    InputStream inFromClient;

    OutputStream outToClient;

    byte[] request = new byte[1024];

    private static final String PROXY_HOST = "localhost";


    private ProxyServer server;


    public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {


        this.clientSocket = clientSocket;


        this.server = proxyServer;

        try {
            clientSocket.setSoTimeout(5000);
            inFromClient = clientSocket.getInputStream();
            outToClient = clientSocket.getOutputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {

        /**
         * To do
         * Process the requests from a client. In particular,
         * (1) Check the request type, only process GET request and ignore others
         * (2) Write log.
         * (3) If the url of GET request has been cached, respond with cached content
         * (4) Otherwise, call method proxyServertoClient to process the GET request
         *
         */
        DataInputStream dis = new DataInputStream(inFromClient);
        try {
            while (true) {
                if (dis.read(request) <= 0) continue;

                String s = new String(request);
                if (!s.startsWith("GET")) continue;
                String[] split = s.split(System.lineSeparator());
                if (split.length == 0) continue;
                String header = split[0];
                String[] splitHeader = header.split(" ");
                if (splitHeader.length == 0) continue;
                String url = splitHeader[1];

                InetAddress address = InetAddress.getByName(new URL(url).getHost());

                server.writeLog(address.getHostAddress() + " " + url);
                System.out.println(s);

                System.out.println(s);
                if (server.cache.containsKey(url)) {
                    sendCachedInfoToClient(server.getCache(url));
                } else {
                    proxyServertoClient(url, address, request);
                }

                Thread.sleep(1);
            }
        } catch (IOException | InterruptedException e){
            // ignore
        } finally {
            close(inFromClient);
            close(outToClient);
            close(clientSocket);
        }
    }

    private void close(Closeable closeable){
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void proxyServertoClient(String url, InetAddress address, byte[] clientRequest) throws IOException {

        /**
         * To do
         * (1) Create a socket to connect to the web server (default port 80)
         * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
         * (3) Use a while loop to read all responses from web server and send back to client
         * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
         * (5) close file, and sockets.
         */
        // Create Buffered output stream to write to cached copy of file
        String fileName = "cached" + File.separator + generateRandomFileName() + ".dat";

        // Note: Creating socket/filestream inside of try catch automatically closes the resources at the end (called a try-with-resources).
        try (Socket toWebServerSocket = new Socket(address, 80)){
            InputStream inFromServer = toWebServerSocket.getInputStream();
            OutputStream outToServer = toWebServerSocket.getOutputStream();

            outToServer.write(clientRequest);
            outToServer.flush();

            try (FileOutputStream fileWriter = new FileOutputStream(fileName)){
                byte[] serverReply = new byte[4096];


                while (inFromServer.read(serverReply) > 0){
                    fileWriter.write(serverReply);
                    outToClient.write(serverReply);
                    fileWriter.flush();
                    outToClient.flush();
                }
            }
        }

        this.server.putCache(url, fileName);
    }



    // Sends the cached content stored in the cache file to the client
    private void sendCachedInfoToClient(String fileName) {

        try {

            byte[] bytes = Files.readAllBytes(Paths.get(fileName));

            outToClient.write(bytes);
            outToClient.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            if (clientSocket != null) {
                clientSocket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    // Generates a random file name
    public String generateRandomFileName() {

        String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; ++i) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}