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

  @Override
  public Integer call() {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println("[Server] Listening on port " + port);

      String END_OF_LINE = "\n";

      while (!serverSocket.isClosed()) {
        try (Socket socket = serverSocket.accept();
             Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader);
             Writer writer =
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
             BufferedWriter out = new BufferedWriter(writer)) {
          System.out.println(
                  "[Server] New client connected from "
                          + socket.getInetAddress().getHostAddress()
                          + ":"
                          + socket.getPort());

          // Run REPL until client disconnects
          while (!socket.isClosed()) {
            // Read response from client
            String clientRequest = in.readLine();

            // If clientRequest is null, the client has disconnected
            // The server can close the connection and wait for a new client
            if (clientRequest == null) {
              socket.close();
              continue;
            }

            // Split user input to parse command (also known as message)
            String[] clientRequestParts = clientRequest.split(" ", 2);

            ClientCommand command = null;
            try {
              command = ClientCommand.valueOf(clientRequestParts[0]);
            } catch (Exception e) {
              // Do nothing
            }

            // Prepare response
            String response = null;

            // Handle request from client
            switch (command) {
              case SND_MSG -> {
                if (clientRequestParts.length < 2) {
                  System.out.println(
                          "[Server] " + command + " command received without <msg> parameter. Replying with "
                                  + ServerCommand.INVALID
                                  + ".");
                  response = ServerCommand.INVALID + " Missing <msg> parameter. Please try again.";
                  break;
                }

                String msg = clientRequestParts[1];

                System.out.println("[Server] Received SND_MSG command with msg: " + msg);
                System.out.println("[Server] Sending MSG to current room");

                //Sending message to all clients using UDP and multicast
                String multicastAddress = "230.0.0.0";
                int multicastPort = 4343;
                try (MulticastSocket multicastSocket = new MulticastSocket()) {
                  InetAddress group = InetAddress.getByName(multicastAddress);
                  byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
                  DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicastPort);
                  multicastSocket.send(packet);
                  System.out.println("[Server] Message sent to multicast group");
                } catch (IOException e) {
                  System.out.println("[Server] Error sending multicast message: " + e.getMessage());
                }


                response = ServerCommand.SENDING_MSG + " Sending to current room !";
              }
              case null, default -> {
                System.out.println(
                        "[Server] Unknown command sent by client, reply with "
                                + ServerCommand.INVALID
                                + ".");
                response = ServerCommand.INVALID + " Unknown command. Please try again.";
              }
            }

            // Send response to client
            out.write(response + END_OF_LINE);
            out.flush();
          }

          System.out.println("[Server] Closing connection");
        } catch (IOException e) {
          System.out.println("[Server] IO exception: " + e);
        }
      }

    } catch (IOException e) {
      System.err.println("[Server] Error: " + e.getMessage());
      return 1;
    }
    return 0;
  }
}
