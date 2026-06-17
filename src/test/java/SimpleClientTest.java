import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SimpleClientTest {

    @Test
    void formatsMessagesUsingJsonProtocol() {
        String payload = SimpleClient.formatMessage("hello");

        assertEquals(new Message("question", "hello").toJson(), payload);
    }
}
