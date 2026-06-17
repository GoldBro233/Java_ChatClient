import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int DEFAULT_PORT = 8080;
    private static final Path DEFAULT_DB_PATH = Path.of("data", "chat.db");

    private final int port;
    private final MessageRepository messageRepository;
    private final ClientAliasGenerator aliasGenerator;
    private final ConcurrentHashMap<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private UUID selectedClientId;
    private ServerSocket serverSocket;

    public Server(int port) throws SQLException {
        this(port, new MessageRepository(DEFAULT_DB_PATH), new ClientAliasGenerator());
    }

    public Server(int port, MessageRepository messageRepository, ClientAliasGenerator aliasGenerator) {
        this.port = port;
        this.messageRepository = messageRepository;
        this.aliasGenerator = aliasGenerator;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        Thread.startVirtualThread(this::replLoop);

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                UUID clientId = UUID.randomUUID();
                String alias = aliasGenerator.nextAlias();
                ClientSession session = new ClientSession(clientId, alias, socket);
                sessions.put(clientId, session);
                System.out.println(clientLabel(session) + " connected");
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
                    saveMessage(session, "inbound", message);
                    System.out.println("[" + clientLabel(session) + "] " + message.content());
                } catch (IOException e) {
                    break;
                }
            }
        } finally {
            sessions.remove(session.getId());
            session.close();
            System.out.println(clientLabel(session) + " disconnected");
        }
    }

    private void replLoop() {
        java.io.BufferedReader console = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        System.out.println("Type /list, /switch <uuid>, /history, /history <uuid>, /history-all, /quit, or a message to send to the selected client.");

        try {
            String line;
            while (running && (line = console.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!handleCommand(line, System.out)) {
                    break;
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

    public boolean handleCommand(String line, PrintStream output) {
        if (line.equals("/list")) {
            listClients(output);
            return true;
        }
        if (line.startsWith("/switch ")) {
            switchClient(line.substring(8).trim(), output);
            return true;
        }
        if (line.equals("/history")) {
            printHistorySummaries(output);
            return true;
        }
        if (line.equals("/history-all")) {
            printAllHistory(output);
            return true;
        }
        if (line.startsWith("/history ")) {
            printHistory(line.substring(9).trim(), output);
            return true;
        }
        if (line.equals("/quit")) {
            stop();
            return false;
        }
        sendToSelectedClient(line, output);
        return true;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void listClients(PrintStream output) {
        if (sessions.isEmpty()) {
            output.println("No clients connected.");
            return;
        }
        for (ClientSession s : sessions.values()) {
            String marker = s.getId().equals(selectedClientId) ? " (selected)" : "";
            output.println("  " + clientLabel(s) + marker);
        }
    }

    private void switchClient(String rawId, PrintStream output) {
        UUID id = parseClientId(rawId, output, "/switch <uuid>");
        if (id == null) {
            return;
        }
        if (sessions.containsKey(id)) {
            selectedClientId = id;
            output.println("Switched to " + clientLabel(sessions.get(id)));
        } else {
            output.println("Client " + id + " not found. Use /list to see connected clients.");
        }
    }

    private void printHistory(String rawId, PrintStream output) {
        UUID id = parseClientId(rawId, output, "/history <uuid>");
        if (id == null) {
            return;
        }
        try {
            List<StoredMessage> history = messageRepository.findByClientId(id);
            if (history.isEmpty()) {
                output.println("No history for client " + id + ".");
                return;
            }
            String alias = history.get(0).clientAlias();
            output.println("History for " + alias + " (" + id + ")");
            for (StoredMessage message : history) {
                output.println(message.createdAt() + " [" + message.direction() + "/" + message.type() + "] "
                        + message.content());
            }
        } catch (SQLException e) {
            output.println("Unable to load history: " + e.getMessage());
        }
    }

    private void printHistorySummaries(PrintStream output) {
        try {
            List<HistorySummary> summaries = messageRepository.findHistorySummaries();
            if (summaries.isEmpty()) {
                output.println("No historical clients.");
                return;
            }
            output.println("Historical clients:");
            for (HistorySummary summary : summaries) {
                output.println(summary.lastMessageAt() + " " + summary.clientAlias() + " ("
                        + summary.clientId() + ") messages=" + summary.messageCount());
            }
        } catch (SQLException e) {
            output.println("Unable to load history summaries: " + e.getMessage());
        }
    }

    private void printAllHistory(PrintStream output) {
        try {
            List<StoredMessage> history = messageRepository.findAll();
            if (history.isEmpty()) {
                output.println("No history.");
                return;
            }
            output.println("All history:");
            for (StoredMessage message : history) {
                output.println(message.createdAt() + " " + message.clientAlias() + " (" + message.clientId()
                        + ") [" + message.direction() + "/" + message.type() + "] " + message.content());
            }
        } catch (SQLException e) {
            output.println("Unable to load all history: " + e.getMessage());
        }
    }

    private void sendToSelectedClient(String line, PrintStream output) {
        if (selectedClientId == null) {
            output.println("No client selected. Use /switch <uuid> first.");
            return;
        }
        ClientSession session = sessions.get(selectedClientId);
        if (session != null && session.isConnected()) {
            Message message = new Message("answer", line);
            session.sendMessage(message);
            saveMessage(session, "outbound", message);
        } else {
            output.println("Client " + selectedClientId + " is no longer connected.");
            selectedClientId = null;
        }
    }

    private UUID parseClientId(String rawId, PrintStream output, String usage) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            output.println("Usage: " + usage);
            return null;
        }
    }

    private void saveMessage(ClientSession session, String direction, Message message) {
        try {
            messageRepository.save(new ChatMessage(session.getId(), session.getAlias(), direction, message));
        } catch (SQLException e) {
            System.err.println("Unable to save message: " + e.getMessage());
        }
    }

    private String clientLabel(ClientSession session) {
        return session.getAlias() + " (" + session.getId() + ")";
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Server server = new Server(port);
        server.start();
    }
}
