package info.kgeorgiy.ja.boin.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
public class HelloUDPNonblockingClient implements NewHelloClient {
    private static final int BUFFER_SIZE = 1 << 8;
    private final Charset clientCharset = StandardCharsets.UTF_8;
    private final long SELECTOR_TIMEOUT = 50;
    private final int MAX_ATTEMPTS = 10;
    private Selector selector;
    private final Map<SocketAddress, ChannelInfo> channels = new ConcurrentHashMap<>();

    private class RequestInfo {
        final int threadNum;
        final String message;
        int attempts = 0;
        long lastSendTime;

        RequestInfo(int threadNum, Request request) {
            this.threadNum = threadNum;
            this.message = request.template().replace("$", String.valueOf(threadNum));
        }

        boolean timeLimitReached(long currentTime) {
            return currentTime - lastSendTime > SELECTOR_TIMEOUT;
        }

        boolean isMaxAttemptsReached() {
            return attempts >= MAX_ATTEMPTS;
        }
    }

    private class ChannelInfo {
        final DatagramChannel channel;
        final Queue<RequestInfo> queue = new LinkedList<>();
        RequestInfo currentRequest;

        ChannelInfo(DatagramChannel channel) {
            this.channel = channel;
        }

        boolean hasActiveRequest() {
            return currentRequest != null;
        }
    }

    @Override
    public void newRun(List<Request> requests, int threads) {
        try {
            selector = Selector.open();

            for (int threadNum = 1; threadNum <= threads; threadNum++) {
                for (Request request : requests) {
                    SocketAddress address = new InetSocketAddress(request.host(), request.port());

                    channels.computeIfAbsent(address, addr -> {
                        try {
                            DatagramChannel channel = DatagramChannel.open();
                            channel.configureBlocking(false);
                            channel.connect(address);
                            channel.register(selector, SelectionKey.OP_READ);
                            return new ChannelInfo(channel);
                        } catch (IOException e) {
                            System.err.println("Channel creation failed for " + addr);
                            return null;
                        }
                    });

                    channels.get(address).queue.add(new RequestInfo(threadNum, request));
                }
            }

            channels.values().forEach(this::sendNextRequest);
            processRequests();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void sendNextRequest(ChannelInfo channelInfo) {
        if (channelInfo.queue.isEmpty() || channelInfo.hasActiveRequest()) return;

        channelInfo.currentRequest = channelInfo.queue.peek();
        ByteBuffer buffer = ByteBuffer.wrap(channelInfo.currentRequest.message.getBytes(clientCharset));

        try {
            channelInfo.channel.send(buffer, channelInfo.channel.getRemoteAddress());
        } catch (IOException e) {
            System.err.println("Error while sending...");
        }
        channelInfo.currentRequest.attempts++;
        channelInfo.currentRequest.lastSendTime = System.currentTimeMillis();
    }


    private void processRequests() throws IOException {
        ByteBuffer receiveBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        while (!allRequestsCompleted()) {
            long currentTime = System.currentTimeMillis();

            processTimeouts(currentTime);

            selector.select(key -> {
                if (key.isReadable()) {
                    receiveBuffer.clear();
                    DatagramChannel channel = (DatagramChannel) key.channel();
                    try {
                        SocketAddress sa = channel.receive(receiveBuffer);
                        receiveBuffer.flip();
                        String response = clientCharset.decode(receiveBuffer).toString();
                        handleResponse(sa, response);
                    } catch (IOException e) {
                        System.err.println("Unable to receive data");
                    }
                }
            }, SELECTOR_TIMEOUT);
        }
    }

    private void processTimeouts(long currentTime) {
        for (ChannelInfo channelInfo : channels.values()) {
            if (!channelInfo.hasActiveRequest()) continue;

            RequestInfo req = channelInfo.currentRequest;
            if (!req.timeLimitReached(currentTime)) continue;

            if (req.isMaxAttemptsReached()) {
                System.err.println("Request timeout: " + req.message);
                completeCurrentRequest(channelInfo, false);
            } else {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(req.message.getBytes(clientCharset));
                    channelInfo.channel.send(buffer, channelInfo.channel.getRemoteAddress());
                    req.attempts++;
                    req.lastSendTime = currentTime;
                } catch (IOException e) {
                    System.err.println("Resend failed: " + e.getMessage());
                }
            }
        }
    }

    private void handleResponse(SocketAddress address, String response) {
        ChannelInfo channelInfo = channels.get(address);
        if (channelInfo == null || !channelInfo.hasActiveRequest()) return;

        if (response.contains(channelInfo.currentRequest.message)) {
            System.out.println(response);
            completeCurrentRequest(channelInfo, true);
            sendNextRequest(channelInfo);
        }
    }

    private void completeCurrentRequest(ChannelInfo channelInfo, boolean success) {
        if (channelInfo.hasActiveRequest()) {
            channelInfo.queue.poll();
            channelInfo.currentRequest = null;
        }
    }

    private boolean allRequestsCompleted() {
        for (ChannelInfo channelInfo : channels.values()) {
            if (!channelInfo.queue.isEmpty() || channelInfo.hasActiveRequest()) {
                return false;
            }
        }
        return true;
    }

    private void close() {
        try {
            if (selector != null) selector.close();
        } catch (IOException e) {
            System.err.println("Error closing selector: " + e.getMessage());
        }
        for (ChannelInfo channelInfo : channels.values()) {
            try {
                if (channelInfo.channel != null) channelInfo.channel.close();
            } catch (IOException e) {
                System.err.println("Error closing channel: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: <host> <port> <prefix> <threads> <requests>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String prefix = args[2];
        int threads = Integer.parseInt(args[3]);
        int requests = Integer.parseInt(args[4]);

        List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            requestList.add(new Request(host, port, prefix));
        }

        new HelloUDPNonblockingClient().newRun(requestList, threads);
    }
}