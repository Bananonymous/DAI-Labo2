package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

/**
 * Client implementation of the application.<br>
 * <br>
 * The <code>Client</code>s will connect to a <code>Server</code> and will then be able to chat with each other either
 * via the global chat or private rooms they create through
 * <code>CREATE_ROOM</code>/<code>JOIN_ROOM</code>/<code>LEAVE_ROOM</code> commands.<br>
 * <br>
 * <code>Client</code>s connects to the <code>Server</code> through TCP to send their messages, receive global chat
 * through UDP and private room chats through the same TCP Socket used for sending.<br>
 * <br>
 * Implements <code>Runnable</code><br>.
 * Uses Buffered inputs and outputs.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see Server
 * @see ch.heigvd.dai.commands.ClientCommand
 * @see ch.heigvd.dai.commands.ServerCommand
 */
public class Client implements Runnable {
    private final String host;
    private final String broadcastAddress = "230.0.0.0";
    private final String NETWROK_INTERFACE = "lo";
    int BUFFER_SIZE = 256;
    private final int TCPport;
    private final int UDPport;
    private boolean done;

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Default Constructor for the class.
     *
     * @param host      Represents the host (<code>Server</code>) to connect to
     * @param TCPport   The TCP port to use, default 4242
     * @param UDPport   The UDP port to use, default 4343
     */
    public Client(String host, int TCPport, int UDPport) {
        this.host = host;
        this.TCPport = TCPport;
        this.UDPport = UDPport;
        done = false;
    }

    /**
     * Main function of the class, used to run the <code>Client</code>.<br>
     * <br>
     * Loops infinitely if command <code>QUIT</code> is not entered or until the connection is stopped from
     * <code>Server</code> (server shutdown).<br>
     * <br>
     * Uses multi-treading to listen and send data over TCP and UDP ports.
     * If an <code>Exception</code> is raised, calls the <code>shutdown()</code> method.
     *
     * @see Client#shutdown()
     */
    @Override
    public void run() {
        try {
            this.client = new Socket(host, TCPport);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            Thread udpListener = new Thread(new UDPListener());
            udpListener.setDaemon(true);
            udpListener.start();

            InputHandler inHandler = new InputHandler();
            Thread inputThread = new Thread(inHandler);
            inputThread.setDaemon(true);
            inputThread.start();

            while (!done) {
                String inMessage = in.readLine();
                System.out.println(inMessage);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    /**
     * Used to properly terminate a <code>Client</code>.<br>
     * <br>
     * Can be called by the <code>Client</code> directly through the <code>QUIT</code> command, in response to a
     * <code>Server</code> shutdown or as an <code>Exception</code> handling method.
     *
     * @see Client#run() Client.run()
     * @see InputHandler#run()
     * @see UDPListener#run()
     */
    protected void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // We simply ignore as we are shutting down the client.
            // Could possibly mark function as throws IOException if we wanted to use/get that information in the calling program.
        }
    }

    /**
     * Encapsulating the <code>Client</code> input handling in a <code>Class</code> for better readability. <br>
     * <br>
     * Implements <code>Runnable</code>.
     *
     * @author Nicolas Carbonara
     * @version 1.0
     * @since 1.0
     */
    protected class InputHandler implements Runnable {

        /**
         * Main function of the class, used to get the user inputs and send them to the <code>Server</code>.<br>
         * <br>
         * Loops infinitely if command <code>QUIT</code> is not entered or until the connection is stopped from
         * <code>Server</code> (server shutdown).<br>
         * If an <code>Exception</code> is raised, calls the <code>shutdown()</code> method.
         *
         * @see Client#shutdown()
         */
        @Override
        public void run() {
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (!done) {
                    String clientInput = inputReader.readLine();

                    if (clientInput.equalsIgnoreCase("QUIT")) {
                        out.println(clientInput);
                        inputReader.close();
                        shutdown();
                    } else {
                        out.println(clientInput);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    /**
     * Encapsulating the <code>Client</code> output handling in a <code>Class</code> for better readability and for the
     * possibility to have multiple output streams for the same <code>Client</code>.<br>
     * <br>
     * Implements <code>Runnable</code>.
     *
     * @author Nicolas Carbonara
     * @version 1.0
     * @since 1.0
     */
    protected class UDPListener implements Runnable {

        /**
         * Main function of the class, used to listen to incoming messages sent by the <code>Server</code> to the global chat.<br>
         * <br>
         * Loops infinitely if command <code>QUIT</code> is not entered or until the connection is stopped from
         * <code>Server</code> (server shutdown).<br>
         * If an <code>Exception</code> is raised, calls the <code>shutdown()</code> method.
         *
         * @see Client#shutdown()
         */
        @Override
        public void run() {
            try (MulticastSocket udpSocket = new MulticastSocket(UDPport)) {
                InetAddress group = InetAddress.getByName(broadcastAddress);
                NetworkInterface networkInterface = NetworkInterface.getByName(NETWROK_INTERFACE);

                udpSocket.joinGroup(new InetSocketAddress(group, UDPport), networkInterface);

                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

                while (!done) {
                    udpSocket.receive(packet);
                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(receivedMessage);
                }

                udpSocket.leaveGroup(new InetSocketAddress(group, UDPport), networkInterface);
            } catch (IOException e) {
                System.out.println("Error initializing UDP socket : " + e.getMessage());
                shutdown();
            }
        }
    }
}