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
                new Thread(() -> handleClient(clientSocket)).start();
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
            writer.println("Please authenticate or register using: AUTH <username> <password> or REGISTER <username> <password>");

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
                                boolean userAlreadyLoggedIn = false;
                                Iterator<Session> iterator = activeSessions.values().iterator();
                                while (iterator.hasNext()) {
                                    Session session = iterator.next();
                                    if (session.username.equals(user)) {
                                        if (session.isExpired()) {
                                            iterator.remove();
                                        } else {
                                            userAlreadyLoggedIn = true;
                                        }
                                    }
                                }
                                if (userAlreadyLoggedIn) {
                                    writer.println("ERROR: User already logged in.");
                                    break; // Exit the switch case
                                }
                                // Create new session
                                token = UUID.randomUUID().toString();
                                Session newSession = new Session(user, token, Instant.now().plus(Duration.ofMinutes(30)));
                                activeSessions.put(token, newSession);
                            } finally {
                                lock.unlock();
                            }
                            username = user;
                            writer.println("AUTH_SUCCESS " + token);
                            writer.println("Available commands: JOIN <room>, LEAVE, MSG <message>, QUIT, LIST");
                        } else {
                            writer.println("ERROR: Authentication failed.");
                        }
                        break;
                    
                    case "REGISTER":
                        if (username != null) {
                            writer.println("ERROR: You are already authenticated");
                            break;
                        }
                        if (parts.length < 2) {
                            writer.println("ERROR: Invalid REGISTER command");
                            break;
                        }
                        String[] regParts = parts[1].split(" ", 2);
                        if (regParts.length < 2) {
                            writer.println("ERROR: Invalid format. Use: REGISTER <username> <password>");
                            break;
                        }
                        String newUser = regParts[0];
                        String newPass = regParts[1];
                        if (!newUser.matches("^[a-zA-Z0-9]{3,20}$")) {
                            writer.println("ERROR: Username must be 3-20 alphanumeric characters");
                            break;
                        }
                        if (newPass.length() < 4) {
                            writer.println("ERROR: Password must be at least 4 characters");
                            break;
                        }
                        
                        lock.lock();

                        try {
                        if (userCredentials.containsKey(newUser)) {
                            writer.println("ERROR: Username already exists");
                            break;
                        }
                        
                        // Add to file
                        try (FileWriter fw = new FileWriter("users.txt", true);
                            BufferedWriter bw = new BufferedWriter(fw);
                            PrintWriter out = new PrintWriter(bw)) {
                            out.println(newUser + ":" + newPass);
                            userCredentials.put(newUser, newPass);
                            writer.println("REGISTER_SUCCESS Account created");
                        } catch (IOException e) {
                            writer.println("ERROR: Registration failed. Please try again");
                            System.err.println("Error saving user: " + e.getMessage());
                        }
                    } finally {
                        lock.unlock();
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
                        } finally {
                            lock.unlock();
                        }
                        writer.println("Joined room: " + roomName);
                        currentRoom.broadcast(username + " has joined the room.", username);
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

    private static class Session {
        String username;
        String token;
        Instant expiry;

        Session(String username, String token, Instant expiry) {
            this.username = username;
            this.token = token;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
