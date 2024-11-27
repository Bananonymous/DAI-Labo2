package ch.heigvd.dai.commands;

/**
 * An enum representing the possible commands a <code>Client</code> can send to a <code>Server</code>.
 *
 * @author Léon Surbeck
 * @author Nicolas Carbonara
 * @version 1.0
 * @see ch.heigvd.dai.Client
 */
public enum ClientCommand {
  MSG,
  RECV_MSG,
  CREATE_ROOM,
  JOIN_ROOM,
  LEAVE_ROOM,
  NICK,
  HELP,
  QUIT
}
