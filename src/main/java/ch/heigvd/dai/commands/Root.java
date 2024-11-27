package ch.heigvd.dai.commands;

import picocli.CommandLine;


/**
 * Parent class for the PicoCLI API, represents the entry point for any subcommands we define.<br>
 * Used to centralise common parameters between subcommands.<br>
 *
 * @author Professor Ludovic Delafontaine
 * @author Professor Hadrien Louis
 * @author Assitant Silvestri Geraud
 * @version 1.0
 * @see ClientCmd
 * @see ServerCmd
 */
@CommandLine.Command(
    description = "YASMA - Yet Another Simple Messaging Application",
    version = "1.0.0",
    subcommands = {
      ClientCmd.class,
      ServerCmd.class,
    },
    scope = CommandLine.ScopeType.INHERIT,
    mixinStandardHelpOptions = true)
public class Root {
    @CommandLine.Option(
            names = {"-t", "--TCPport"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "4242")
    private int TCPport;

    @CommandLine.Option(
            names = {"-u", "--UDPport"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "4343")
    private int UDPport;

    protected int getTCPport() { return TCPport; }
    protected int getUDPport() { return UDPport; }
}
