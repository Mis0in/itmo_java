package info.kgeorgiy.ja.boin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {
    private static final long SELECTOR_TIMEOUT = 250;
    private Thread serverThread;
    private final Charset serverCharset = StandardCharsets.UTF_8;
    private Selector selector;
    private DatagramChannel serverChannel;
    private ExecutorService service;
    private final ConcurrentLinkedQueue<HelloResponse> responses = new ConcurrentLinkedQueue<>();

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            service = Executors.newFixedThreadPool(threads);

            serverChannel = DatagramChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("ERROR: Unable to create a server");
            return;
        }
        runServer();
    }

    private void runServer() {
        serverThread = new Thread(() -> {
            int size;
            try {
                size = serverChannel.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                System.err.println("Unable to get buffer size.");
                return;
            }

            ByteBuffer buffer = ByteBuffer.allocate(size);
            while (!Thread.interrupted()) {
                try {
                    selector.select(key -> {
                        if (key.isReadable()) {
                            handleRead(key, buffer);
                        }
                        if (key.isWritable()) {
                            handleWrite(key);
                        }
                    }, SELECTOR_TIMEOUT);
                } catch (IOException e) {
                    System.err.println("Error while selecting: " + e.getMessage());
                }
            }
        });
        serverThread.start();
    }

    private void handleRead(SelectionKey key, ByteBuffer buffer) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        try {
            buffer.clear();
            SocketAddress address = channel.receive(buffer);

            buffer.flip();
            String request = serverCharset.decode(buffer).toString();
            service.submit(() -> handleRequest(request, address));
        } catch (Exception e) {
            System.err.println("Unable to receive buffer. Skipping...");
        }
    }

    private void handleWrite(SelectionKey key) {
        HelloResponse response = responses.peek();
        if (response == null) {
            removeInterestOp(key, SelectionKey.OP_WRITE);
            return;
        }

        try {
            serverChannel.send(response.bytes(), response.address());
        } catch (IOException e) {
            System.err.println("Error sending: " + e.getMessage());
        } finally {
            responses.poll();
            if (responses.isEmpty()) {
                removeInterestOp(key, SelectionKey.OP_WRITE);
            }
        }
    }

    private void handleRequest(String s, SocketAddress address) {
        String response = "Hello, " + s;
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes(serverCharset));
        responses.add(new HelloResponse(responseBuffer, address));

        SelectionKey key = serverChannel.keyFor(selector);
        addInterestOp(key, SelectionKey.OP_WRITE);
    }

    @SuppressWarnings("MagicConstant")
    private void addInterestOp(SelectionKey key, int op) {
        int currentOps = key.interestOps();
        if ((currentOps & op) == 0) {
            key.interestOps(currentOps | op);
            selector.wakeup();
        }
    }

    @SuppressWarnings("MagicConstant")
    private static void removeInterestOp(SelectionKey key, int op) {
        key.interestOps(key.interestOps() & ~op);
    }
    
    @Override
    public void close() {
        serverThread.interrupt();
        service.shutdownNow();
        try {
            selector.close();
        } catch (IOException e) {
            System.err.println("Unable to close selector");
        }
        try {
            serverChannel.close();
        } catch (IOException e) {
            System.err.println("Unable to close server channel");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("expected 2 arguments");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);

        try (HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
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

record HelloResponse(ByteBuffer bytes, SocketAddress address) {
}