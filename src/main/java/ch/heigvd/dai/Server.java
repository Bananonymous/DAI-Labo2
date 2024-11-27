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

/**
 * Server implementation of the application.<br>
 * <br>
 * The <code>Server</code>s will listen to <code>Client</code>s connection and will accept them to allow multiple
 * <code>Client</code>s to chat with each other either via the global chat or private rooms they through the
 * <code>CREATE_ROOM</code>/<code>JOIN_ROOM</code>/<code>LEAVE_ROOM</code> commands.<br>
 * <br>
 * <code>Server</code>s connects to the <code>Clients</code> through TCP to receive their messages, and then resend
 * those messages to global chat through UDP or private room chats through the same TCP Socket used for receiving.<br>
 * <br>
 * Implements <code>Runnable</code><br>.
 * Uses Buffered inputs and outputs.<br>
 * Manages one <code>Thread</code> per TCP connection and one <code>Thread</code> for UDP through an
 * <code>Executor</code> pool.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see Client
 * @see ch.heigvd.dai.commands.ClientCommand
 * @see ch.heigvd.dai.commands.ServerCommand
 */
public class Server implements Runnable {
    private ServerSocket server;
    private final int TCPport;
    private final int UDPport;
    private final ArrayList<ConnectionHandler> connections;
    private final Map<String, ArrayList<ConnectionHandler>> chatrooms;
    private final String broadcastAddress;

    private boolean done;

    /**
     * Default Constructor for the class.
     *
     * @param TCPport           Port used for the receiving of messages from clients
     * @param UDPport           Port used for the multiclass of the UDP global channel
     * @param broadcastAddress  IP address used for the multiclass of the UDP global channel
     */
    public Server(int TCPport, int UDPport, String broadcastAddress) {
        this.TCPport = TCPport;
        this.UDPport = UDPport;
        this.broadcastAddress = broadcastAddress;
        connections = new ArrayList<>();
        chatrooms = new HashMap<>();
        done = false;
    }

    /**
     * Main function of the class, used to run the <code>Server</code>>.<br>
     * <br>
     * Will loop infinitely if command <code>QUIT</code> is not entered.<br>
     * <br>
     * Uses multi-treading to listen and send data over TCP and UDP ports.<br>
     * If an <code>Exception</code> is raised, calls the <code>shutdown()</code> method.
     *
     * @see Server#shutdown()
     */
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

    /**
     * Broadcast (multicast) the messages to the global channel.
     *
     * @param message   A message received from a client
     */
    public void broadcast(String message) {
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

    /**
     * Used to properly terminate the <code>Server</code>.<br>
     * <br>
     * Can be called by the <code>Server</code> directly through the <code>QUIT</code> command or as an
     * <code>Exception</code> handling method.
     *
     * @see Server#run() Server.run()
     */
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

    /**
     * Represents the <code>Client</code>s' connections to the <code>Server</code>.<br>
     * <br>
     * Implements <code>Runnable</code>.
     *
     * @author Nicolas Carbonara
     * @version 1.0
     * @since 1.0
     */
    protected class ConnectionHandler implements Runnable {

        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        /**
         * Default Constructor for the class.
         *
         * @param client The socket created by the <code>Server</code>
         */
        protected ConnectionHandler(Socket client) {
            this.client = client;
        }

        /**
         * Main function of the class, used to interact with the <code>Client</code>s.<br>
         * <br>
         * Loops infinitely if command <code>QUIT</code> is not entered. If entered, will <code>shutdown</code> the
         * connection with the distant <code>Client</code>.
         * If an <code>Exception</code> is raised, calls the <code>shutdown()</code> method.
         *
         * @see ConnectionHandler#shutdown()
         */
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
                    String[] splittedMessage = clientInput.split(" ", 2);
                    try {
                        command = ClientCommand.valueOf(splittedMessage[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        out.println("Unrecognised command, please try again");
                        continue;
                    }

                    if (splittedMessage.length < 2 && !clientInput.toUpperCase().startsWith("QUIT") && !clientInput.toUpperCase().startsWith("HELP") &&
                            !clientInput.toUpperCase().startsWith("LEAVE_ROOM")) {
                        out.println("Not enough parameters, please try again !");
                        continue;
                    } else if (splittedMessage.length == 2) {
                        if (command == ClientCommand.CREATE_ROOM || command == ClientCommand.JOIN_ROOM) {
                            if (splittedMessage[1] == null) {
                                out.println("No room name provided.");
                                continue;
                            } else if (roomName != null) {
                                out.println("You already are in a room, please leave it using LEAVE_ROOM before joining another one.");
                                continue;
                            } else {
                                roomName = splittedMessage[1];
                            }
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
                            if (!chatrooms.containsKey(roomName) && roomName != null) {
                                out.println(roomName + " successfully created.");
                                System.out.println(nickname + " created the room " + roomName);
                                chatrooms.putIfAbsent(roomName, new ArrayList<>());
                            }
                            if(roomName == null) {
                                out.println("No room name provided.");
                                break;
                            }
                            out.println("You joined the room " + roomName);
                            System.out.println(nickname + " joined the room " + roomName);
                            chatrooms.get(roomName).add(this);
                        }

                        case JOIN_ROOM -> {
                            if (!chatrooms.containsKey(roomName)) {
                                out.println("Room " + roomName + " does not exist.");
                                break;
                            }
                            out.println("You joined the room " + roomName);
                            System.out.println(nickname + " joined the room " + roomName);
                            chatrooms.get(roomName).add(this);
                        }

                        case LEAVE_ROOM -> {
                            if (!chatrooms.containsKey(roomName) && roomName != null) {
                                out.println("Room " + roomName + " does not exist.");
                                break;
                            }
                            if(roomName == null) {
                                out.println("You are not in a room.");
                                break;
                            }
                            out.println("You left the room " + roomName);
                            System.out.println(nickname + " left the room " + roomName);
                            chatrooms.get(roomName).remove(this);
                            roomName = null;
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

        /**
         * Used to properly terminate a <code>Client</code>'s connection.<br>
         * <br>
         * Can be called by the <code>Client</code> directly through the <code>QUIT</code> command, in response to a
         * <code>Server</code> shutdown or as an <code>Exception</code> handling method.
         *
         * @see Client.InputHandler#run()
         * @see Client.UDPListener#run()
         * @see ConnectionHandler#run()
         */
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

        /**
         * Displays the help menu for each of the available <code>ClientCommand</code>.
         */
        protected void help() {
            out.println("Usage:");
            out.println("  " + ClientCommand.MSG + " [room name] <msg> - Send message to the target room if specified else sends it to global chat.");
            out.println("  " + ClientCommand.CREATE_ROOM + " <room name> - Creates the specified room or join it if already existing.");
            out.println("  " + ClientCommand.NICK + " - Allows you to change your nickname");
            out.println("  " + ClientCommand.HELP + " - Display this help message.");
            out.println("  " + ClientCommand.QUIT + " - Close the connection to the server.");
        }

        /**
         * Sends the client message over TCP.<br>
         * Used when chatting in a private room.
         * @param message   The message received from the client
         */
        protected void sendMessage(String message) {
            out.println(message);
        }
    }
}