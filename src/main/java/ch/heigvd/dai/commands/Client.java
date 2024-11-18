package ch.heigvd.dai.commands;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

enum ClientCommand {
  MSG,
  RECV_MSG,
  CREATE_ROOM,
  HELP,
  QUIT
}

enum ServerCommand {
  OK,
  SENDING_MSG,
  INVALID
}

@CommandLine.Command(name = "client", description = "Start the client part of YASMA.")
public class Client implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Host to connect to.",
      required = true)
  protected String host;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "4242")
  protected int port;


  @Override
  public Integer call() {
    String END_OF_LINE = "\n";

    System.out.println("[Client] Connecting to " + host + ":" + port + "...");

    try (Socket socket = new Socket(host, port);
         Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(reader);
         Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
         BufferedWriter out = new BufferedWriter(writer)) {

      System.out.println("[Client] Connected to " + host + ":" + port);
      System.out.println();

      // Display help message
      help();

      // Start a thread to listen for multicast messages
      Thread multicastListener = new Thread(this::listenToMulticast);
      multicastListener.setDaemon(true);
      multicastListener.start();

      // Main loop for user commands
      while (!socket.isClosed()) {
        System.out.print("> ");

        // Read user input
        Reader inputReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader bir = new BufferedReader(inputReader);
        String userInput = bir.readLine();

        try {
          // Split user input to parse command
          String[] userInputParts = userInput.split(" ", 2);
          ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());

          String request = null;
          switch (command) {
            case MSG -> {
              String msg = userInputParts[1];
              request = ClientCommand.MSG + " " + msg;
            }
            case QUIT -> {
              socket.close();
              continue;
            }
            case HELP -> help();
          }

          if (request != null) {
            out.write(request + END_OF_LINE);
            out.flush();
          }
        } catch (Exception e) {
          System.out.println("Invalid command. Please try again.");
        }

        // Read server response
        String serverResponse = in.readLine();
        if (serverResponse == null) {
          socket.close();
          continue;
        }

        // Handle server response
        System.out.println("[Server] " + serverResponse);
      }

      System.out.println("[Client] Closing connection and quitting...");
    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e.getMessage());
    }

    return 0;
  }

  // Multicast listening thread
  private void listenToMulticast() {
    String multicastAddress = "230.0.0.0";
    int multicastPort = 4343;

    try (MulticastSocket multicastSocket = new MulticastSocket(multicastPort)) {
      InetAddress group = InetAddress.getByName(multicastAddress);
      multicastSocket.joinGroup(group);
      while (true) {
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        System.out.println("[Client] Listening to multicast messages...");

        multicastSocket.receive(packet);

        String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("[Multicast] Received message: " + received);
      }
    } catch (IOException e) {
      System.out.println("[Client] Error in multicast listener: " + e.getMessage());
    }
  }


  private static void help() {
    System.out.println("Usage:");
    System.out.println("  " + ClientCommand.MSG + " <msg> - Send message in current room.");
    System.out.println("  " + ClientCommand.QUIT + " - Close the connection to the server.");
    System.out.println("  " + ClientCommand.HELP + " - Display this help message.");
  }
}
