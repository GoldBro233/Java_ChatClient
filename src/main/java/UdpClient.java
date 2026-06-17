import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpClient {

    private final String host;
    private final int port;

    public UdpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(host);

        System.out.println("UDP client ready -> " + host + ":" + port);
        System.out.println("Type a message, or /quit to exit.");

        Thread.startVirtualThread(() -> receiveLoop(socket));

        java.io.BufferedReader console = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        String line;
        System.out.print("> ");
        while ((line = console.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                System.out.print("> ");
                continue;
            }
            if (line.equals("/quit")) {
                System.out.println("Disconnected.");
                break;
            }

            byte[] data = line.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            System.out.print("> ");
        }

        socket.close();
    }

    private void receiveLoop(DatagramSocket socket) {
        byte[] buffer = new byte[1024];
        try {
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                System.out.println("\n[Reply] " + message);
                System.out.print("> ");
            }
        } catch (java.net.SocketException e) {
            // socket closed
        } catch (Exception e) {
            System.out.println("\nReceive error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 2020;
        new UdpClient(host, port).start();
    }
}
