import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ClientTest {

    @Test
    void parsesValidServerMessage() throws Exception {
        Message message = Client.parseServerMessage("{\"type\":\"answer\",\"content\":\"hello\"}");

        assertEquals("answer", message.type());
        assertEquals("hello", message.content());
    }

    @Test
    void invalidServerMessageBecomesIoException() {
        assertThrows(IOException.class, () -> Client.parseServerMessage("plain text"));
    }
}
