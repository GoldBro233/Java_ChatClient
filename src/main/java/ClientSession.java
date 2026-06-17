import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class ClientSession {

    private final UUID id;
    private final String alias;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile boolean connected;

    public ClientSession(UUID id, String alias, Socket socket) throws IOException {
        this.id = id;
        this.alias = alias;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.connected = true;
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isConnected() {
        return connected;
    }

    public Message readMessage() throws IOException {
        String line = in.readLine();
        if (line == null) {
            connected = false;
            return null;
        }
        try {
            return Message.fromJson(line);
        } catch (IllegalArgumentException e) {
            connected = false;
            throw new IOException("Invalid message from client", e);
        }
    }

    public void sendMessage(Message message) {
        out.println(message.toJson());
    }

    public void close() {
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
