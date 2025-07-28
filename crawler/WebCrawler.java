package info.kgeorgiy.ja.boin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebCrawler is an implementation of {@link NewCrawler} interface that recursively downloads web pages
 * and extracts links from them, supporting parallel downloading and processing.
 */
public class WebCrawler implements NewCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadService;
    private final ExecutorService extractService;

    public WebCrawler(Downloader downloader, int download, int extract, int perHost) {
        this.downloader = downloader;
        this.downloadService = Executors.newFixedThreadPool(download);
        this.extractService = Executors.newFixedThreadPool(extract);
    }

    /**
     * Constructs a new WebCrawler instance with specified parameters.
     */
    @Override
    public Result download(String url, int depthLimit, List<String> excludes) {
        Set<String> knownUrls = ConcurrentHashMap.newKeySet();
        Set<String> successes = ConcurrentHashMap.newKeySet();
        Map<String, IOException> failures = new ConcurrentHashMap<>();
        Queue<String> urlsToDownload = new ConcurrentLinkedQueue<>();

        if (isSuitable(url, excludes, knownUrls)) urlsToDownload.add(url);

        Phaser barrier = new Phaser(1);
        for (int depth = 0; depth < depthLimit; depth++) {
            List<Document> documents = Collections.synchronizedList(new ArrayList<>());

            downloadAll(urlsToDownload, barrier, successes, documents, failures);

            if (depth == depthLimit - 1) break;

            Queue<String> extractedUrls = new ConcurrentLinkedQueue<>();
            extractAll(excludes, documents, barrier, extractedUrls, knownUrls);
            urlsToDownload = extractedUrls;
        }
        return new Result(new ArrayList<>(successes), failures);
    }

    /**
     * Extracts URLs and files from given documents in parallel, using <code>extractService</code>
     */
    private void extractAll(List<String> excludes, List<Document> documents, Phaser barrier, Queue<String> extractedUrls, Set<String> knownUrls) {
        documents.forEach(document -> {
            barrier.register();
            extractService.submit(() -> {
                try {
                    document.extractLinks().stream()
                            .filter(link -> isSuitable(link, excludes, knownUrls))
                            .forEach(extractedUrls::add);
                } catch (IOException ignored) {
                } finally {
                    barrier.arriveAndDeregister();
                }
            });
        });
        barrier.arriveAndAwaitAdvance();
    }

    /**
     * Download URLs in parallel, using <code>downloadService</code>
     */
    private void downloadAll(Queue<String> urls, Phaser barrier, Set<String> successes, List<Document> documents, Map<String, IOException> failures) {
       urls.forEach(url -> {
            barrier.register();
            downloadService.submit(() -> {
                try {
                    documents.add(downloader.download(url));
                    successes.add(url);
                } catch (IOException e) {
                    failures.put(url, e);
                } finally {
                    barrier.arriveAndDeregister();
                }
            });
        });
        barrier.arriveAndAwaitAdvance();
    }

    /**
     * Ensures that given url's host does not contain substring from <code>excluded</code> list
     */
    private boolean isSuitable(String url, List<String> excludes, Set<String> knownUrls) {
        try {
            String host = URLUtils.getHost(url);
            return excludes.stream().noneMatch(host::contains) && knownUrls.add(url);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void close() {
        downloadService.shutdownNow();
        extractService.shutdownNow();
    }

    /**
     * Main method for command-line execution of the WebCrawler.
     * Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]
     *
     * @param args command line arguments:
     *             args[0] - starting URL
     *             args[1] - depth limit (default: 1)
     *             args[2] - download threads (default: 1)
     *             args[3] - extract threads (default: 1)
     *             args[4] - per host limit (default: 1)
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        Downloader downloader;
        try {
            downloader = new CachingDownloader(10);
        } catch (IOException e) {
            System.err.println("Unable to create Caching Downloader");
            return;
        }

        List<Integer> intArgs = Arrays.stream(args).skip(1).map(Integer::parseInt).toList();
        try (WebCrawler crawler = new WebCrawler(downloader, intArgs.get(1), intArgs.get(2), intArgs.get(3))) {
            crawler.download(args[0], intArgs.getFirst(), List.of());
        } catch (Exception e) {
            System.err.println("Error while trying to download url");
        }
    }
}
