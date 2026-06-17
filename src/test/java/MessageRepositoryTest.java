import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsMessagesByClientIdInInsertionOrder() throws Exception {
        Path dbPath = tempDir.resolve("chat.db");
        MessageRepository repository = new MessageRepository(dbPath);
        UUID clientId = UUID.randomUUID();

        repository.save(new ChatMessage(clientId, "Alice", "inbound", new Message("question", "hello")));
        repository.save(new ChatMessage(clientId, "Alice", "outbound", new Message("answer", "world")));
        repository.save(new ChatMessage(UUID.randomUUID(), "Bob", "inbound", new Message("question", "ignored")));

        List<StoredMessage> history = repository.findByClientId(clientId);

        assertEquals(2, history.size());
        assertEquals("Alice", history.get(0).clientAlias());
        assertEquals("inbound", history.get(0).direction());
        assertEquals("question", history.get(0).type());
        assertEquals("hello", history.get(0).content());
        assertEquals("outbound", history.get(1).direction());
        assertEquals("answer", history.get(1).type());
        assertEquals("world", history.get(1).content());
        assertEquals(true, Files.exists(dbPath));
    }

    @Test
    void listsHistorySummariesByMostRecentMessage() throws Exception {
        MessageRepository repository = new MessageRepository(tempDir.resolve("chat.db"));
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        repository.save(new ChatMessage(aliceId, "Alice", "inbound", new Message("question", "first")));
        repository.save(new ChatMessage(bobId, "Bob", "inbound", new Message("question", "second")));
        repository.save(new ChatMessage(aliceId, "Alice", "outbound", new Message("answer", "third")));

        List<HistorySummary> summaries = repository.findHistorySummaries();

        assertEquals(2, summaries.size());
        assertEquals(aliceId, summaries.get(0).clientId());
        assertEquals("Alice", summaries.get(0).clientAlias());
        assertEquals(2, summaries.get(0).messageCount());
        assertEquals(bobId, summaries.get(1).clientId());
        assertEquals("Bob", summaries.get(1).clientAlias());
        assertEquals(1, summaries.get(1).messageCount());
    }

    @Test
    void loadsAllMessagesInInsertionOrder() throws Exception {
        MessageRepository repository = new MessageRepository(tempDir.resolve("chat.db"));
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        repository.save(new ChatMessage(aliceId, "Alice", "inbound", new Message("question", "first")));
        repository.save(new ChatMessage(bobId, "Bob", "outbound", new Message("answer", "second")));

        List<StoredMessage> messages = repository.findAll();

        assertEquals(2, messages.size());
        assertEquals(aliceId, messages.get(0).clientId());
        assertEquals("first", messages.get(0).content());
        assertEquals(bobId, messages.get(1).clientId());
        assertEquals("second", messages.get(1).content());
    }
}
