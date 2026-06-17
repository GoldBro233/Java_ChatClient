import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SimpleClient {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: SimpleClient <host> <port> <message>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String message = args[2];

        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            out.write((formatMessage(message) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            out.flush();
            System.out.println("Sent to " + host + ":" + port + " -> " + message);
        }
    }

    public static String formatMessage(String content) {
        return new Message("question", content).toJson();
    }
}
