package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;

import ch.heigvd.dai.Server;
import picocli.CommandLine;

/**
 * A PicoCLI subcommand of <code>Root</code> used to start a server application that will allow <code>Client</code>s to connect.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see Root
 * @see ClientCmd
 * @see Server
 */
@CommandLine.Command(name = "server", description = "Start the server part of YASMA.")
public class ServerCmd implements Callable<Integer> {
  @CommandLine.ParentCommand protected Root parent;

  protected String multicastAddress = "230.0.0.0";

  @Override
  public Integer call() {
    System.out.println("[Server] Listening on port " + parent.getTCPport());
    System.out.println("[Server] Broadcasting on address " + multicastAddress + " port " + parent.getUDPport());
    Server server = new Server(parent.getTCPport(), parent.getUDPport(), multicastAddress);
    server.run();

    return 0;
  }
}
