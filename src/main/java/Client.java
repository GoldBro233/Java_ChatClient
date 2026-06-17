import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to server at " + host + ":" + port);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            Thread receiver = Thread.startVirtualThread(() -> receiveLoop(in));

            sendLoop(out);

            receiver.interrupt();
        }
    }

    private void sendLoop(PrintWriter out) {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        try {
            String line;
            System.out.print("> ");
            while ((line = console.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    System.out.print("> ");
                    continue;
                }
                if (line.equals("/quit")) {
                    System.out.println("Disconnected.");
                    break;
                }
                Message message = new Message("question", line);
                out.println(message.toJson());
                System.out.println("Sent.");
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.err.println("Input error: " + e.getMessage());
        }
    }

    private void receiveLoop(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message message = Message.fromJson(line);
                System.out.println("\n[Answer] " + message.content());
                System.out.print("> ");
            }
            System.out.println("\nServer closed connection.");
        } catch (IOException e) {
            System.out.println("\nConnection lost: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        new Client(host, port).start();
    }
}
