import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class AiRoom extends Room {
    private final String prompt;
    private final List<String> messageHistory = new CopyOnWriteArrayList<>();

    public AiRoom(String name, String prompt) {
        super(name);
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void addMessage(String message) {
        messageHistory.add(message);
    }

    @Override
    public void addClient(String username, PrintWriter writer) {
        super.addClient(username, writer);
        // Send message history to new client
        for (String msg : messageHistory) {
            writer.println(msg);
        }
    }

   public String generateResponse() {
        HttpClient client = HttpClient.newHttpClient();
        String ollamaUrl = "http://localhost:11434/api/generate";
        String model = "llama3";

        StringBuilder fullPrompt = new StringBuilder(prompt + "\n\n");
        for (String msg : messageHistory) {
            fullPrompt.append(msg).append("\n");
        }

        String command = String.format(
            "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
            model,
            fullPrompt.toString()
                .replace("\\", "\\\\")  // Escape backslashes
                .replace("\"", "\\\"") // Escape quotes
                .replace("\n", "\\n")   // Escape newlines
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(command))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                return extractResponse(jsonResponse);
            } else {
                System.err.println("Ollama API Error: " + response.body());
                return "Sorry, I encountered an error.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error connecting to AI service.";
        }
    }

    private String extractResponse(String json) {
        // First try quoted response format
        int start = json.indexOf("\"response\":\"");
        if (start != -1) {
            start += 12; // Length of "\"response\":\""
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return unescapeJson(json.substring(start, end));
            }
        }

        // Fallback to unquoted format
        start = json.indexOf("\"response\":");
        if (start != -1) {
            start += 11; // Length of "\"response\":"
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end != -1) {
                return json.substring(start, end).trim();
            }
        }

        return "Could not parse AI response";
    }

    private String unescapeJson(String text) {
        return text.replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\\\", "\\");
    }
}