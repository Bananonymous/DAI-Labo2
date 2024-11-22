package ch.heigvd.dai.commands;

import picocli.CommandLine;

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
