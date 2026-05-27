import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {

    private final int id;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile boolean connected;

    public ClientSession(int id, Socket socket) throws IOException {
        this.id = id;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.connected = true;
    }

    public int getId() {
        return id;
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
        return Message.fromJson(line);
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
