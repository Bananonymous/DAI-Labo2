# DAI 2024-2025 - Practical Work 2: YASMA - Yet Another Simple Messaging Application

Based on the Java Template [Java TCP programming](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/main/12-java-tcp-programming).

## Project Overview

This project is a simple messaging application designed as part of the DAI 2024-2025 practical work. The system allows multiple clients to communicate concurrently, creating rooms for private discussions between users. We also aim to implement encryption to enhance the security of the conversations.

## Features

- **Multi-client support**: Manage multiple clients simultaneously using client concurrency.
- **Private discussion rooms**: Users can create and join private discussion rooms to communicate securely with each other.
- **Encryption (Optional)**: If possible, we will add encryption to ensure the confidentiality of the messages exchanged between users.

# How to Use / Run on Your Own Machine

This part explains how to run the `dai-lab02-yasma` container image on your local machine.

## Prerequisites

1. Install Docker on your system. Follow the instructions at [Get Docker](https://docs.docker.com/get-docker/).

## Running the Application

### Pull the Image

First, pull the container image from GitHub Packages:

```bash
docker pull ghcr.io/bananonymous/dai-lab02-yasma:latest
```

### Running the Server

To run the application in server mode, execute the following command:

```bash
docker run --rm -it --net=host ghcr.io/bananonymous/dai-lab02-yasma:latest server
```

### Running the Client

To run the application in client mode, execute the following command:

```bash
docker run --rm -it --net=host ghcr.io/bananonymous/dai-lab02-yasma:latest client --host=localhost
```

You should replace localhost with the actual IP of the server running the application (Or keep it if you are running the client and server on the same machine)

### Explanation of `--net=host`

The `--net=host` flag is necessary because the application uses UDP multicast, which is not natively supported within Docker's default network configurations. Using `--net=host` allows the container to use the host's network stack, enabling proper multicast communication.

## Notes

- The `--rm` flag ensures that the container is automatically removed after it stops running.
- The `-it` flag allows interactive input and output.

If you encounter any issues, please open an issue in the [GitHub repository](https://github.com/Bananonymous/dai-lab02-yasma/issues).

## Protocol

The protocol can be found in the [Wiki](https://github.com/Bananonymous/DAI-Labo2/wiki) of this repository and describes how our application should work.
As most (if not all) of the Client's *Commands* have the same error returning patern, we decided not to copy-paste the same diagram for each of them thus only having one *Error Example*.

## Future Improvements

- **Encryption**: We plan to implement end-to-end encryption for all messages to enhance security.
- **User Authentication**: Adding user authentication features to ensure only authorized users can join rooms.

## Contributors

- Carbonara Nicolas
- Léon Surbeck
