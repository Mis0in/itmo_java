package info.kgeorgiy.ja.boin.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloClient.Request;

import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class HelloRequest implements Runnable {
    private final List<Request> requests;
    private final int threadNumber;
    private final Charset clientCharset = StandardCharsets.UTF_8;
    private final int threadTL = 250;

    public HelloRequest(List<Request> request, int i) {
        this.requests = request;
        this.threadNumber = i;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            requests.forEach(request ->
            {
                String messageString = buildMessage(request.template());
                byte[] message = messageString.getBytes();
                DatagramPacket packet;
                try {
                    packet = new DatagramPacket(message, message.length, InetAddress.getByName(request.host()), request.port());
                    socket.setSoTimeout(threadTL);
                } catch (SocketException | UnknownHostException e) {
                    System.err.println("Unable to init requests");
                    return;
                }

                while (true) {
                    try {
                        socket.send(packet);
                        byte[] received = new byte[socket.getReceiveBufferSize()];
                        DatagramPacket responsePacket = new DatagramPacket(received, received.length);
                        socket.receive(responsePacket);
                        String response = new String(
                                responsePacket.getData(),
                                responsePacket.getOffset(),
                                responsePacket.getLength(),
                                clientCharset
                        );

                        if (checkAnswer(response, messageString)) break;
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            });
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        }
    }

    private static boolean checkAnswer(String response, String messageString) {
        if (response.equals("Hello, " + messageString)) {
            System.out.println(response);
            return true;
        }
        return false;
    }

    private String buildMessage(String template) {
        return template.replaceAll("\\$", String.valueOf(threadNumber));
    }
}
