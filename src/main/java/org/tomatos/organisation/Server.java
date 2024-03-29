package org.tomatos.organisation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
             shutDown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler handler : connections) {
            if (handler != null) {
                handler.sendMessage(message);
                System.out.println(message);
            }
        }
    }

    public void shutDown() {
        done = true;
        try {
            if (!server.isClosed()) {
                server.close();
            }

            for(ConnectionHandler ch : connections) {
                ch.disconnect();
            }
        } catch (IOException e) {
            //ignore
        }
    }


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                sendMessage("Use // for commands example: //quit //nick ");
                sendMessage("Enter a nickname: ");
                nickName = in.readLine();
                System.out.println(nickName + " connected !");
                broadcast(nickName + " join the chat");
                String message;
                
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("//nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickName + " --> " + messageSplit[1]);
                            System.out.println(nickName + " --> " + messageSplit[1]);
                            nickName = messageSplit[1];
                            sendMessage("Changed nickname to " + nickName);
                        } else {
                            sendMessage("No nickname provided");
                        }
                    } else if (message.equalsIgnoreCase("//quit")) {
                        broadcast(nickName + " left the chat");
                        disconnect();
                    } else {
                        broadcast(nickName + ": " + message);
                    }
                }
            } catch (IOException e) {
                disconnect();
            }
        }

        public void disconnect() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
