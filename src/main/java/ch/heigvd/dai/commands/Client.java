package ch.heigvd.dai.commands;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import picocli.CommandLine;

enum ClientCommand {
  SND_MSG,
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
      defaultValue = "6433")
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

      // Run REPL until user quits
      while (!socket.isClosed()) {
        // Display prompt
        System.out.print("> ");

        // Read user input
        Reader inputReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
        BufferedReader bir = new BufferedReader(inputReader);
        String userInput = bir.readLine();

        try {
          // Split user input to parse command (also known as message)
          String[] userInputParts = userInput.split(" ", 2);
          ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase());

          // Prepare request
          String request = null;

          switch (command) {
            case SND_MSG -> {
              String msg = userInputParts[1];

              request = ClientCommand.SND_MSG + " " + msg;
            }
            case QUIT -> {
              socket.close();
              continue;
            }
            case HELP -> help();
          }

          if (request != null) {
            // Send request to server
            out.write(request + END_OF_LINE);
            out.flush();
          }
        } catch (Exception e) {
          System.out.println("Invalid command. Please try again.");
          continue;
        }

        // Read response from server and parse it
        String serverResponse = in.readLine();

        // If serverResponse is null, the server has disconnected
        if (serverResponse == null) {
          socket.close();
          continue;
        }

        // Split response to parse message (also known as command)
        String[] serverResponseParts = serverResponse.split(" ", 2);

        ServerCommand message = null;
        try {
          message = ServerCommand.valueOf(serverResponseParts[0]);
        } catch (IllegalArgumentException e) {
          // Do nothing
        }

        // Handle response from server
        switch (message) {
          case OK -> {
            // As we know from the server implementation, the message is always the second part
            String helloMessage = serverResponseParts[1];
            System.out.println(helloMessage);
          }
          case INVALID -> {
            if (serverResponseParts.length < 2) {
              System.out.println("Invalid message. Please try again.");
              break;
            }

            String invalidMessage = serverResponseParts[1];
            System.out.println(invalidMessage);
          }
          case null, default ->
                  System.out.println("Invalid/unknown command sent by server, ignore.");
        }
      }

      System.out.println("[Client] Closing connection and quitting...");
    } catch (Exception e) {
      System.out.println("[Client] Exception: " + e);
    }


    return 0;
  }

  private static void help() {
    System.out.println("Usage:");
    System.out.println("  " + ClientCommand.SND_MSG + " <msg> - Send message in current room.");
    System.out.println("  " + ClientCommand.QUIT + " - Close the connection to the server.");
    System.out.println("  " + ClientCommand.HELP + " - Display this help message.");
  }
}
