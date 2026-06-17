import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class UdpServer {

    private static final int DEFAULT_PORT = 2020;
    private static final int BUFFER_SIZE = 1024;

    private final int port;
    private volatile boolean running = true;

    public UdpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("UDP server started on port " + port);

        Thread.startVirtualThread(() -> replLoop(socket));

        byte[] buffer = new byte[BUFFER_SIZE];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                System.out.println("[" + sender + "] " + message);
            } catch (java.net.SocketException e) {
                if (running) {
                    System.err.println("Receive error: " + e.getMessage());
                }
            }
        }

        socket.close();
        System.out.println("Server stopped.");
    }

    private void replLoop(DatagramSocket socket) {
        java.io.BufferedReader console = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        System.out.println("Type: <ip:port> <message>  |  /quit");

        try {
            String line;
            while (running && (line = console.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("/quit")) {
                    running = false;
                    socket.close();
                    break;
                }

                int spaceIdx = line.indexOf(' ');
                if (spaceIdx == -1) {
                    System.out.println("Usage: <ip:port> <message>");
                    continue;
                }

                String target = line.substring(0, spaceIdx);
                String message = line.substring(spaceIdx + 1);

                int colonIdx = target.lastIndexOf(':');
                if (colonIdx == -1) {
                    System.out.println("Usage: <ip:port> <message>");
                    continue;
                }

                String host = target.substring(0, colonIdx);
                int port = Integer.parseInt(target.substring(colonIdx + 1));

                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length,
                        java.net.InetAddress.getByName(host), port);
                socket.send(packet);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("REPL error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new UdpServer(port).start();
    }
}
