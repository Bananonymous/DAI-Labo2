# DAI 2024-2025 - Practical Work 2: YASMA - Yet Another Simple Messaging System

Based on the Java Template [Java TCP programming](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/main/12-java-tcp-programming).

## Project Overview

This project is a simple messaging system designed as part of the DAI 2024-2025 practical work. The system allows multiple clients to communicate concurrently, creating rooms for private discussions between users. We also aim to implement encryption to enhance the security of the conversations.

## Features

- **Multi-client support**: Manage multiple clients simultaneously using client concurrency.
- **Private discussion rooms**: Users can create and join private discussion rooms to communicate securely with each other.
- **Encryption (Optional)**: If possible, we will add encryption to ensure the confidentiality of the messages exchanged between users.

## Setup and Installation

1. **Clone the repository**
   ```sh
   git clone https://github.com/heig-vd-dai-course/messaging-system.git
   ```

2. **Navigate to the project directory**
   ```sh
   cd messaging-system
   ```

3. **Install dependencies** (if applicable)
   - Ensure you have Python 3 installed (or any other language the project uses).
   - Install any required packages using:
     ```sh
     pip install -r requirements.txt
     ```

## Running the Application

- **Server**: Start the server to handle client connections.
  ```sh
  python server.py
  ```

- **Client**: Start a client to connect to the server.
  ```sh
  python client.py
  ```

## Usage

- **TODO**: What happens when TODO 

## Future Improvements

- **Encryption**: We plan to implement end-to-end encryption for all messages to enhance security.
- **User Authentication**: Adding user authentication features to ensure only authorized users can join rooms.

## Contributors

- Carbonara Nicolas
- Léon Surbeck
