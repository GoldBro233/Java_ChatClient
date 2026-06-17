import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageRepository {

    private final String jdbcUrl;

    public MessageRepository(Path dbPath) throws SQLException {
        try {
            Path parent = dbPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (java.io.IOException e) {
            throw new SQLException("Unable to create database directory", e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initialize();
    }

    public void save(ChatMessage chatMessage) throws SQLException {
        String sql = """
                INSERT INTO messages (client_id, client_alias, direction, type, content)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, chatMessage.clientId().toString());
            statement.setString(2, chatMessage.clientAlias());
            statement.setString(3, chatMessage.direction());
            statement.setString(4, chatMessage.message().type());
            statement.setString(5, chatMessage.message().content());
            statement.executeUpdate();
        }
    }

    public List<StoredMessage> findByClientId(UUID clientId) throws SQLException {
        String sql = """
                SELECT id, client_id, client_alias, direction, type, content, created_at
                FROM messages
                WHERE client_id = ?
                ORDER BY id
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clientId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StoredMessage> messages = new ArrayList<>();
                while (resultSet.next()) {
                    messages.add(new StoredMessage(
                            resultSet.getLong("id"),
                            UUID.fromString(resultSet.getString("client_id")),
                            resultSet.getString("client_alias"),
                            resultSet.getString("direction"),
                            resultSet.getString("type"),
                            resultSet.getString("content"),
                            Instant.parse(resultSet.getString("created_at"))));
                }
                return messages;
            }
        }
    }

    public List<HistorySummary> findHistorySummaries() throws SQLException {
        String sql = """
                SELECT client_id, client_alias, COUNT(*) AS message_count, MAX(created_at) AS last_message_at
                FROM messages
                GROUP BY client_id, client_alias
                ORDER BY MAX(id) DESC
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<HistorySummary> summaries = new ArrayList<>();
            while (resultSet.next()) {
                summaries.add(new HistorySummary(
                        UUID.fromString(resultSet.getString("client_id")),
                        resultSet.getString("client_alias"),
                        resultSet.getInt("message_count"),
                        Instant.parse(resultSet.getString("last_message_at"))));
            }
            return summaries;
        }
    }

    public List<StoredMessage> findAll() throws SQLException {
        String sql = """
                SELECT id, client_id, client_alias, direction, type, content, created_at
                FROM messages
                ORDER BY id
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<StoredMessage> messages = new ArrayList<>();
            while (resultSet.next()) {
                messages.add(new StoredMessage(
                        resultSet.getLong("id"),
                        UUID.fromString(resultSet.getString("client_id")),
                        resultSet.getString("client_alias"),
                        resultSet.getString("direction"),
                        resultSet.getString("type"),
                        resultSet.getString("content"),
                        Instant.parse(resultSet.getString("created_at"))));
            }
            return messages;
        }
    }

    private void initialize() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    client_id TEXT NOT NULL,
                    client_alias TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    type TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
                )
                """;
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_messages_client_id ON messages (client_id, id)");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
