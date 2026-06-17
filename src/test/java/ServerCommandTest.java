import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void printsHistoryForUuidClientId() throws Exception {
        UUID clientId = UUID.randomUUID();
        MessageRepository repository = new MessageRepository(tempDir.resolve("chat.db"));
        repository.save(new ChatMessage(clientId, "Alice", "inbound", new Message("question", "hello")));
        repository.save(new ChatMessage(clientId, "Alice", "outbound", new Message("answer", "hi")));
        Server server = new Server(0, repository, new ClientAliasGenerator());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        server.handleCommand("/history " + clientId, new PrintStream(output));

        String text = output.toString();
        assertTrue(text.contains("History for Alice (" + clientId + ")"));
        assertTrue(text.contains("[inbound/question] hello"));
        assertTrue(text.contains("[outbound/answer] hi"));
    }

    @Test
    void historyWithoutClientIdPrintsPersistedClientSummaries() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        MessageRepository repository = new MessageRepository(tempDir.resolve("chat.db"));
        repository.save(new ChatMessage(aliceId, "Alice", "inbound", new Message("question", "hello")));
        repository.save(new ChatMessage(bobId, "Bob", "inbound", new Message("question", "hi")));
        repository.save(new ChatMessage(aliceId, "Alice", "outbound", new Message("answer", "answer")));
        Server server = new Server(0, repository, new ClientAliasGenerator());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        server.handleCommand("/history", new PrintStream(output));

        String text = output.toString();
        assertTrue(text.contains("Historical clients:"));
        assertTrue(text.contains("Alice (" + aliceId + ") messages=2"));
        assertTrue(text.contains("Bob (" + bobId + ") messages=1"));
    }

    @Test
    void historyAllPrintsEveryPersistedMessage() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        MessageRepository repository = new MessageRepository(tempDir.resolve("chat.db"));
        repository.save(new ChatMessage(aliceId, "Alice", "inbound", new Message("question", "hello")));
        repository.save(new ChatMessage(bobId, "Bob", "outbound", new Message("answer", "hi")));
        Server server = new Server(0, repository, new ClientAliasGenerator());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        server.handleCommand("/history-all", new PrintStream(output));

        String text = output.toString();
        assertTrue(text.contains("All history:"));
        assertTrue(text.contains("Alice (" + aliceId + ") [inbound/question] hello"));
        assertTrue(text.contains("Bob (" + bobId + ") [outbound/answer] hi"));
    }
}
