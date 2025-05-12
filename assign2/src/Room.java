import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa uma sala de chat, gerindo participantes e broadcast de mensagens.
 */
public class Room {
    private final String name;
    private final Map<String, PrintWriter> clients = new HashMap<>();
    private final Lock roomLock = new ReentrantLock();

    /**
     * Cria uma sala com o nome especificado.
     * @param name nome da sala
     */
    public Room(String name) {
        this.name = name;
    }

    /**
     * Retorna o nome da sala.
     * @return nome da sala
     */
    public String getName() {
        return name;
    }

    /**
     * Adiciona um cliente à sala.
     * @param username nome de utilizador do cliente
     * @param writer  writer para enviar mensagens ao cliente
     */
    public void addClient(String username, PrintWriter writer) {
        roomLock.lock();
        try {
            clients.put(username, writer);
        } finally {
            roomLock.unlock();
        }
    }

    /**
     * Remove um cliente da sala.
     * @param username nome de utilizador do cliente a remover
     */
    public void removeClient(String username) {
        roomLock.lock();
        try {
            clients.remove(username);
        } finally {
            roomLock.unlock();
        }
    }

    /**
     * Envia uma mensagem a todos os clientes da sala, exceto o remetente.
     * @param message conteúdo da mensagem
     * @param sender  nome do utilizador que enviou a mensagem
     */
    public void broadcast(String message, String sender) {
        roomLock.lock();
        try {
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                if (!entry.getKey().equals(sender)) {
                    entry.getValue().println(message);
                }
            }
        } finally {
            roomLock.unlock();
        }
    }
}
