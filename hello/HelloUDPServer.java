package info.kgeorgiy.ja.boin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private Thread serverThread;
    private ExecutorService serverService;
    private DatagramSocket socket;
    private final Charset serverCharset = StandardCharsets.UTF_8;

    @Override
    public void start(int port, int threads) {
        if (serverService != null) {
            System.err.println("Only one server can be started");
            return;
        }
        serverService = Executors.newFixedThreadPool(threads);

        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Unable to start server socket: " + e.getMessage());
            return;
        }
        serverThread = new Thread(() -> {
            for (int i = 0; i < threads; i++) {
                serverService.submit(() -> {
                    try {
                        while (!socket.isClosed() && !serverThread.isInterrupted()) {
                            byte[] buffer = new byte[socket.getReceiveBufferSize()];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                            socket.receive(packet);
                            handleRequest(packet);
                        }
                    } catch (SocketException e) {
                        System.err.println(e.getMessage());
                    } catch (IOException e) {
                        System.err.println("Receiving error: " + e.getMessage());
                    }
                });
            }

        }
        );
        serverThread.start();
    }

    private void handleRequest(DatagramPacket packet) {
        int clientPort = packet.getPort();
        InetAddress address = packet.getAddress();
        String request = new String(
                packet.getData(),
                packet.getOffset(),
                packet.getLength(),
                serverCharset
        );
        byte[] answer = ("Hello, " + request).getBytes(serverCharset);
        try {
            DatagramPacket response = new DatagramPacket(answer, answer.length, address, clientPort);
            socket.send(response);
        } catch (IOException e) {
            System.err.println("Sending error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        socket.close();
        serverService.shutdownNow();
        serverThread.interrupt();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("expected 2 arguments");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);

        try (HelloUDPServer server = new HelloUDPServer()) {
            server.start(port, threads);
            System.out.println("Server started...");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted");
            }
            System.out.println("Server closing...");
        }

    }
}
