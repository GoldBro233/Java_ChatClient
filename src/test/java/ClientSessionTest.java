import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientSessionTest {

    @Test
    void invalidJsonDisconnectsSessionWithIoException() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0);
             Socket clientSocket = new Socket("localhost", serverSocket.getLocalPort());
             Socket serverSideSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            ClientSession session = new ClientSession(UUID.randomUUID(), "Alice", serverSideSocket);
            out.println("plain text");

            assertThrows(IOException.class, session::readMessage);
            assertFalse(session.isConnected());
        }
    }
}
