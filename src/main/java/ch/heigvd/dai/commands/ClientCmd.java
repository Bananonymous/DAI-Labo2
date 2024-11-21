package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;

import ch.heigvd.dai.Client;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start the client part of YASMA.")
public class ClientCmd implements Callable<Integer> {

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

    System.out.println("[Client] Connecting to " + host + " : " + port + "...");

    Client client = new Client(host, port, 4343);
    client.run();

    return 0;
  }
}
