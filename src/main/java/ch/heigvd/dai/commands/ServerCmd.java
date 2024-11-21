package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;

import ch.heigvd.dai.Server;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server part of YASMA.")
public class ServerCmd implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-t", "--TCPport"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "4242")
  protected int TCPport;

  @CommandLine.Option(
          names = {"-u", "--UDPport"},
          description = "Port to use (default: ${DEFAULT-VALUE}).",
          defaultValue = "4343")
  protected int UDPport;

  protected String multicastAddress = "230.0.0.0";

  @Override
  public Integer call() {
    System.out.println("[Server] Listening on port " + TCPport);
    System.out.println("[Server] Broadcasting on address " + multicastAddress + " port " + UDPport);
    Server server = new Server(TCPport, UDPport, multicastAddress);
    server.run();

    return 0;
  }
}
