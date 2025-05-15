import java.time.Duration;
import java.time.Instant;

public class Session {
    String username;
    String token;
    Instant expiry;
    String Room = null;

    Session(String username, String token, Instant expiry) {
        this.username = username;
        this.token = token;
        this.expiry = expiry;
    }

    boolean isExpired() {
        return Instant.now().isAfter(expiry);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCurrentRoom() {
        return Room;
    }

    public void setCurrentRoom(String currentRoom) {
        this.Room = currentRoom;
    }

    public void RenovateExpiry() {
        this.expiry = Instant.now().plus(Duration.ofMinutes(30));
    }
}
