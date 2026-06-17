import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void rejectsInvalidJsonMessages() {
        assertThrows(IllegalArgumentException.class, () -> Message.fromJson("plain text"));
    }

    @Test
    void rejectsMessagesMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> Message.fromJson("{\"type\":\"question\"}"));
        assertThrows(IllegalArgumentException.class, () -> Message.fromJson("{\"content\":\"hello\"}"));
    }

    @Test
    void parsesValidMessage() {
        Message message = Message.fromJson("{\"type\":\"question\",\"content\":\"hello\"}");

        assertEquals("question", message.type());
        assertEquals("hello", message.content());
    }
}
