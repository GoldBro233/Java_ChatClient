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
    private int selectedClientId = -1;
    private ServerSocket serverSocket;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        Thread.startVirtualThread(this::replLoop);

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                int clientId = nextClientId.getAndIncrement();
                ClientSession session = new ClientSession(clientId, socket);
                sessions.put(clientId, session);
                System.out.println("Client-" + clientId + " connected");
                Thread.startVirtualThread(() -> receiveLoop(session));
            } catch (java.net.SocketException e) {
                if (running) {
                    System.err.println("Accept error: " + e.getMessage());
                }
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

    private void replLoop() {
        java.io.BufferedReader console = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        System.out.println("Type /list, /switch <id>, /quit, or a message to send to the selected client.");

        try {
            String line;
            while (running && (line = console.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.equals("/list")) {
                    if (sessions.isEmpty()) {
                        System.out.println("No clients connected.");
                    } else {
                        for (ClientSession s : sessions.values()) {
                            String marker = (s.getId() == selectedClientId) ? " (selected)" : "";
                            System.out.println("  Client-" + s.getId() + marker);
                        }
                    }
                } else if (line.startsWith("/switch ")) {
                    try {
                        int id = Integer.parseInt(line.substring(8).trim());
                        if (sessions.containsKey(id)) {
                            selectedClientId = id;
                            System.out.println("Switched to Client-" + id);
                        } else {
                            System.out.println("Client-" + id + " not found. Use /list to see connected clients.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Usage: /switch <id>");
                    }
                } else if (line.equals("/quit")) {
                    stop();
                    break;
                } else {
                    if (selectedClientId == -1) {
                        System.out.println("No client selected. Use /switch <id> first.");
                    } else {
                        ClientSession session = sessions.get(selectedClientId);
                        if (session != null && session.isConnected()) {
                            session.sendMessage(new Message("answer", line));
                        } else {
                            System.out.println("Client-" + selectedClientId + " is no longer connected.");
                            selectedClientId = -1;
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("REPL error: " + e.getMessage());
            }
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
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Server server = new Server(port);
        server.start();
    }
}
