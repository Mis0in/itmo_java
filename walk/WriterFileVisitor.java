package info.kgeorgiy.ja.boin.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WriterFileVisitor implements FileVisitor<Path>, AutoCloseable {

    private final BufferedWriter out;
    private final byte[] buffer = new byte[1 << 8];
    String hashAlgorithm = "SHA-256";
    final int bytesInHash = 8;
    String hashOnFail = "0".repeat(bytesInHash * 2);

    public WriterFileVisitor(BufferedWriter writer) {
        out = writer;
    }

    public void writeError(String path) {
        try {
            writeHash(hashOnFail, path);
        } catch (IOException e) {
            System.err.println("Unable to write to file");
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        writeError(dir.toString());
        return FileVisitResult.SKIP_SUBTREE;
    }

    protected void writeHash(String hash, String filePath) throws IOException {
        out.write(hash + " " + filePath + System.lineSeparator());
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
            int read;
            while ((read = is.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hash = new StringBuilder();
            for (int i = 0; i < bytesInHash; i++) {
                hash.append(String.format("%02x", hashBytes[i]));
            }
            writeHash(hash.toString(), file.toString());

        } catch (IOException | NoSuchAlgorithmException e) {
            visitFileFailed(file, new IOException(e.getMessage()));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        writeError(file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
