package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;

import ch.heigvd.dai.Client;
import picocli.CommandLine;

/**
 * A PicoCLI subcommand of <code>Root</code> used to start a client application that will connect to the server.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see Root
 * @see ServerCmd
 * @see Client
 */
@CommandLine.Command(name = "client", description = "Start the client part of YASMA.")
public class ClientCmd implements Callable<Integer> {
  @CommandLine.ParentCommand protected Root parent;

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Host to connect to.",
      required = true)
  protected String host;

  @Override
  public Integer call() {

    System.out.println("[Client] Connecting to " + host + " : " + parent.getTCPport() + "...");

    Client client = new Client(host, parent.getTCPport(), parent.getUDPport());
    client.run();

    return 0;
  }
}
