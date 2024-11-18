package ch.heigvd.dai.commands;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server part of YASMA.")
public class Server implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "4242")
  protected int port;

  private void handleClient(Socket socket) {
    try (Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(reader);
         Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
         BufferedWriter out = new BufferedWriter(writer)) {

      while (!socket.isClosed()) {
        String clientRequest = in.readLine();
        if (clientRequest == null) {
          socket.close();
          break;
        }

        String[] clientRequestParts = clientRequest.split(" ", 2);
        ClientCommand command = null;
        try {
          command = ClientCommand.valueOf(clientRequestParts[0]);
        } catch (Exception e) {
          // Invalid command
        }

        String response = null;
        switch (command) {
          case MSG -> {
            if (clientRequestParts.length < 2) {
              response = ServerCommand.INVALID + " Missing <msg> parameter. Please try again.";
            } else {
              String msg = clientRequestParts[1];
              sendMulticastMessage(msg);
              response = ServerCommand.SENDING_MSG + " Sending to current room!";
            }
          }
          case null, default -> response = ServerCommand.INVALID + " Unknown command. Please try again.";
        }

        if (response != null) {
          out.write(response + "\n");
          out.flush();
        }
      }
    } catch (IOException e) {
      System.out.println("[Server] IO exception: " + e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        System.out.println("[Server] Error closing socket: " + e.getMessage());
      }
    }
  }

  private void sendMulticastMessage(String msg) {
    String multicastAddress = "230.0.0.0";
    int multicastPort = 4343;

    try (DatagramSocket multicastSocket = new DatagramSocket()) {
      InetAddress group = InetAddress.getByName(multicastAddress);
      byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicastPort);

      multicastSocket.send(packet);
      System.out.println("[Server] Sent multicast message: " + msg);
    } catch (IOException e) {
      System.out.println("[Server] Error sending multicast message: " + e.getMessage());
    }
  }


  @Override
  public Integer call() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("[Server] Listening on port " + port);

      String END_OF_LINE = "\n";

      while (!serverSocket.isClosed()) {
        Socket socket = serverSocket.accept();
        System.out.println("[Server] New client connected from "
                + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

        // Create a new thread for each client
        new Thread(() -> handleClient(socket)).start();
      }
    } catch (IOException e) {
      System.err.println("[Server] Error: " + e.getMessage());
      return 1;
    }
    return 0;
  }
}
