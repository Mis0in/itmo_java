package info.kgeorgiy.ja.boin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements NewHelloClient {
    private final long TIME_PER_THREAD = 2000;

    @Override
    public void newRun(List<Request> requests, int threads) {
        ExecutorService sender = Executors.newFixedThreadPool(threads);

         IntStream.rangeClosed(1, threads)
                  .forEach((i) -> sender.submit(new HelloRequest(requests, i)));

        sender.shutdown();
        awaitRequestsComplete(sender, threads * requests.size() * TIME_PER_THREAD);
    }

    private void awaitRequestsComplete(ExecutorService service, long time) {
        try {
            boolean ignored = service.awaitTermination(time, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Client thread interrupted");
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("expected 5 arguments");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String prefix = args[2];
        int threads = Integer.parseInt(args[3]);
        int requests = Integer.parseInt(args[4]);
        HelloClient client = new HelloUDPClient();
        client.run(host, port, prefix, threads, requests);

    }
}
