package ch.heigvd.dai.commands;

/**
 * An enum representing the possible answers a <code>Server</code> can return to a <code>Client</code> in response to a command received.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see ClientCommand
 * @see ch.heigvd.dai.Server
 */
public enum ServerCommand {
  OK,
  SENDING_MSG,
  INVALID
}
