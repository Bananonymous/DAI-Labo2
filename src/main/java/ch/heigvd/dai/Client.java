package ch.heigvd.dai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

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

    public Client(String host, int TCPport, int UDPport) {
        this.host = host;
        this.TCPport = TCPport;
        this.UDPport = UDPport;
        done = false;
    }

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

    protected class InputHandler implements Runnable {

        @Override
        public void run() {
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (!done) {
                    String clientInput = inputReader.readLine();

                    if (clientInput.equals("QUIT")) {
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

    protected class UDPListener implements Runnable {

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