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
public class Root {}
