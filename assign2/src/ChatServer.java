import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.time.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, String> userCredentials = new HashMap<>();
    private static final Map<String, Session> activeSessions = new HashMap<>();
    private static final Map<String, Room> chatRooms = new HashMap<>();


    private static final Lock lock = new ReentrantLock();

    public static void main(String[] args) {
        loadUserCredentials("users.txt");
        System.out.println("Chat server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().start(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadUserCredentials(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.strip().split(":", 2);
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading user credentials: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        String username = null;
        String token = null;
        Room currentRoom = null;

        try (
                socket;
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            writer.println("Welcome to the Chat Server!");
            writer.println("Please authenticate using: AUTH <username> <password>");
            writer.println("If reconnecting: RECONNECT <token>");

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.strip().split(" ", 2);
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "AUTH":
                        if (parts.length < 2) {
                            writer.println("ERROR: Invalid AUTH command.");
                            break;
                        }
                        String[] credentials = parts[1].split(" ", 2);
                        if (credentials.length < 2) {
                            writer.println("ERROR: Invalid AUTH command.");
                            break;
                        }
                        String user = credentials[0];
                        String pass = credentials[1];
                        if (authenticate(user, pass)) {
                            lock.lock();
                            try {
                                token = UUID.randomUUID().toString();
                                Session session = new Session(user, token, Instant.now().plus(Duration.ofMinutes(30)));
                                activeSessions.put(token, session);
                                username = user;
                                writer.println("AUTH_SUCCESS " + token);
                                writer.println("Available commands: JOIN <room>, LEAVE, MSG <message>, QUIT, LIST");
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            writer.println("ERROR: Authentication failed.");
                        }
                        break;

                    case "LIST":
                        // devolve ao cliente a lista de salas existentes
                        writer.println("ROOMS " + String.join(",", chatRooms.keySet()));
                        break;

                    case "JOIN":
                        if (parts.length < 2) {
                            writer.println("ERROR: Room name required.");
                            break;
                        }
                        String roomName = parts[1];
                        lock.lock();
                        try {
                            currentRoom = chatRooms.computeIfAbsent(roomName, Room::new);
                            currentRoom.addClient(username, writer);
                            Session sess = activeSessions.get(token);
                            if(sess != null) {
                                sess.setCurrentRoom(roomName);
                            }
                        } finally {
                            lock.unlock();
                        }
                        writer.println("Joined room: " + roomName);
                        currentRoom.broadcast(username + " has joined the room.", username);
                        break;

                    case "RECONNECT":
                        if (parts.length < 2) {
                            writer.println("ERROR: Token required.");
                            break;
                        }
                        String tkn = parts[1];
                        lock.lock();
                        try {
                            Session sess = activeSessions.get(tkn);
                            if (sess != null && !sess.isExpired()) {
                                sess.RenovateExpiry();

                                username = sess.username;
                                token = tkn;
                                currentRoom = chatRooms.get(sess.getCurrentRoom());

                                if(currentRoom != null) {
                                    currentRoom.addClient(username, writer);
                                }

                                writer.println("RESUME_OK Welcome back, " + sess.username);
                                writer.println("Available commands: JOIN <room>, LEAVE, MSG <message>, QUIT, LIST");

                                if (currentRoom != null) {
                                    currentRoom.broadcast(username + " has reconnected.", username);
                                }
                            } else {
                                writer.println("ERROR: Invalid or expired token");
                                activeSessions.remove(tkn);
                            }
                        } finally {
                            lock.unlock();
                        }
                        break;

                    case "LEAVE":
                        if (currentRoom != null) {
                            currentRoom.broadcast(username + " has left the room.", username);
                            currentRoom.removeClient(username);
                            writer.println("Left the room.");
                            currentRoom = null;
                        } else {
                            writer.println("ERROR: Not in any room.");
                        }
                        break;

                    case "MSG":
                        if (parts.length < 2) {
                            writer.println("ERROR: Message content required.");
                            break;
                        }
                        if (currentRoom != null) {
                            currentRoom.broadcast(username + ": " + parts[1], username);
                        } else {
                            writer.println("ERROR: Not in any room.");
                        }
                        break;

                    case "QUIT":
                        writer.println("Goodbye!");
                        if (currentRoom != null) {
                            currentRoom.broadcast(username + " has left the room.", username);
                            currentRoom.removeClient(username);
                        }
                        lock.lock();
                        try {
                            activeSessions.remove(token);
                        } finally {
                            lock.unlock();
                        }
                        return;

                    default:
                        writer.println("ERROR: Unknown command.");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error with client: " + e.getMessage());
        } finally {
            if (currentRoom != null && username != null) {
                currentRoom.broadcast(username + " has disconnected.", username);
                currentRoom.removeClient(username);
            }
            if (token != null) {
                lock.lock();
                try {
                    activeSessions.remove(token);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static boolean authenticate(String username, String password) {
        return password.equals(userCredentials.get(username));
    }
}
