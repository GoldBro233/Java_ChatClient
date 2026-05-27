import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int DEFAULT_PORT = 8080;

    private final int port;
    private final ConcurrentHashMap<Integer, ClientSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger nextClientId = new AtomicInteger(1);
    private volatile boolean running = true;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (running) {
                Socket socket = serverSocket.accept();
                int clientId = nextClientId.getAndIncrement();
                ClientSession session = new ClientSession(clientId, socket);
                sessions.put(clientId, session);
                System.out.println("Client-" + clientId + " connected");
                Thread.startVirtualThread(() -> receiveLoop(session));
            }
        }

        shutdown();
    }

    private void receiveLoop(ClientSession session) {
        try {
            while (running && session.isConnected()) {
                try {
                    Message message = session.readMessage();
                    if (message == null) {
                        break;
                    }
                    System.out.println("[Client-" + session.getId() + "] " + message.content());
                } catch (IOException e) {
                    break;
                }
            }
        } finally {
            sessions.remove(session.getId());
            session.close();
            System.out.println("Client-" + session.getId() + " disconnected");
        }
    }

    private void shutdown() {
        System.out.println("Shutting down...");
        for (ClientSession session : sessions.values()) {
            session.close();
        }
        sessions.clear();
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Server server = new Server(port);
        server.start();
    }
}
