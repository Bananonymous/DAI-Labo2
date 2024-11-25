package ch.heigvd.dai;

import ch.heigvd.dai.commands.ClientCommand;
import ch.heigvd.dai.commands.ServerCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ServerSocket server;
    private final int TCPport;
    private final int UDPport;
    private ArrayList<ConnectionHandler> connections;
    private Map<String, ArrayList<ConnectionHandler>> chatrooms;
    private final String broadcastAddress;

    private boolean done;

    public Server(int TCPport, int UDPport, String broadcastAddress) {
        this.TCPport = TCPport;
        this.UDPport = UDPport;
        this.broadcastAddress = broadcastAddress;
        connections = new ArrayList<>();
        chatrooms = new HashMap<>();
        done = false;
    }

    @Override
    public void run() {
        try (ExecutorService threads = Executors.newCachedThreadPool()) {
            server = new ServerSocket(TCPport);
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                threads.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast (String message) {
        try (MulticastSocket multicastSocket = new MulticastSocket(UDPport)) {
            InetAddress group = InetAddress.getByName(broadcastAddress);
            NetworkInterface networkInterface = NetworkInterface.getByName("lo");

            multicastSocket.setNetworkInterface(networkInterface);

            byte[] buf = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, UDPport);

            multicastSocket.send(packet);
            System.out.println("Message sent over multicast: " + message);
        } catch (IOException e) {
            System.out.println("[Server] Error sending multicast message: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            done = true;

            if (!server.isClosed()) {
                server.close();
            }
            // We shut down every client's connection
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // We simply ignore as we are shutting down the server.
            // Could possibly mark function as throws IOException if we wanted to use/get that information in the calling program.
        }
    }

    protected class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        protected ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                // Using PrintWriter for some of its features like auto-flushing
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Welcome ! Please provide a nickname: ");
                nickname = in.readLine();

                // Server logging
                // See stackoverflow for timestamp
                // (https://stackoverflow.com/questions/23068676/how-to-get-current-timestamp-in-string-format-in-java-yyyy-mm-dd-hh-mm-ss)
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                System.out.println(now.format(format) + " " + nickname + " connected.");

                // Announcing to every client the newly arrived friend :D (public chatroom)
                broadcast(nickname + " joined the server, say hi !");

                String message = "";
                String roomName = null;
                ClientCommand command;
                while (!done) {
                    String clientInput = in.readLine();
                    String[] splittedMessage = clientInput.split(" ", 3);
                    try {
                        command = ClientCommand.valueOf(splittedMessage[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        out.println("Unrecognised command, please try again");
                        continue;
                    }

                    if (splittedMessage.length == 3) {
                        roomName = splittedMessage[1];
                        message = splittedMessage[2];
                    } else if (splittedMessage.length < 2 && !clientInput.toUpperCase().startsWith("QUIT") && !clientInput.toUpperCase().startsWith("HELP")) {
                        out.println("Not enough parameters, please try again !");
                        continue;
                    } else if (splittedMessage.length >= 2){
                        if (command == ClientCommand.CREATE_ROOM) {
                            roomName = splittedMessage[1];
                        } else {
                            message = splittedMessage[1];
                        }
                    }

                    switch (command) {
                        case MSG -> {
                            if (roomName == null) {
                                broadcast(nickname + ": " + message);

                                // Server logging
                                System.out.println(this.nickname + " in public channel sends : " + message);
                            } else {
                                for (ConnectionHandler ch : chatrooms.get(roomName)) {
                                    ch.sendMessage(nickname + ": " + message);
                                }

                                // Server logging
                                System.out.println(this.nickname + " in " + roomName + " sends : " + message);
                            }
                        }
                        case CREATE_ROOM -> {
                            if (roomName == null) {
                                out.println("No room name provided.");
                                break;
                            } else if (!chatrooms.containsKey(roomName)) {
                                out.println(roomName + " successfully created.");
                                System.out.println(nickname + " created the room " + roomName);
                                chatrooms.putIfAbsent(roomName, new ArrayList<>());
                            }
                            out.println("You joined the room " + roomName);
                            System.out.println(nickname + " joined the room " + roomName);
                            chatrooms.get(roomName).add(this);
                        }
                        case NICK -> {
                            broadcast(nickname + " renamed themselves to " + splittedMessage[1]);
                            System.out.println(nickname + " renamed themselves to " + splittedMessage[1]);
                            out.println("New identity confirmed, " + splittedMessage[1] + "!");
                            nickname = splittedMessage[1];
                        }
                        case HELP -> help();
                        case QUIT -> {
                            broadcast(nickname + " departed, we will mourn them.");
                            shutdown();
                        }
                        default -> out.println(ServerCommand.INVALID + " Unknown command. Please try again.");
                    }
                }

            } catch (IOException e) {
                shutdown();
            }
        }

        protected void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // We simply ignore as we are shutting down the client connection.
                // Could possibly mark function as throws IOException if we wanted to use/get that information in the calling program.
            }
        }

        protected void help() {
            out.println("Usage:");
            out.println("  " + ClientCommand.MSG + " [room name] <msg> - Send message to the target room if specified else sends it to global chat.");
            out.println("  " + ClientCommand.CREATE_ROOM + " <room name> - Creates the specified room or join it if already existing.");
            out.println("  " + ClientCommand.NICK + " - Allows you to change your nickname");
            out.println("  " + ClientCommand.HELP + " - Display this help message.");
            out.println("  " + ClientCommand.QUIT + " - Close the connection to the server.");
        }

        protected void sendMessage(String message) {
            out.println(message);
        }
    }
}