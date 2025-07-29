package info.kgeorgiy.ja.boin.walk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class Walk {

    protected static void createPath(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.createFile(path);
    }

    protected static void handleInput(Path inputFile, WriterFileVisitor visitor) {
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String readed;
            while ((readed = reader.readLine()) != null) {
                try {
                    Files.walkFileTree(Path.of(readed), visitor);
                } catch (InvalidPathException e) {
                    visitor.writeError(readed);
                }
            }

        } catch (SecurityException e) {
            System.err.println("Unable to open file: " + inputFile);
        } catch (IOException e) {
            System.err.println("Unable handle input: " + inputFile);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            //NOTE: args[i] can be null
            System.err.println("Usage: RecursiveWalk <input path> <output path>");
            return;
        } else if (args[0] == null) {
            System.err.println("Incorrect input: First argument is null");
            return;
        } else if (args[1] == null) {
            System.err.println("Incorrect input: Second argument is null");
            return;
        }

        try {
            Path inPath = Path.of(args[0]);
            Path outPath = Path.of(args[1]);
            if (Files.notExists(outPath)) {
                try {
                    createPath(outPath);
                } catch (IOException e) {
                    System.err.println("Unable to create path: " + outPath);
                    return;
                }
            }
            try (WriterFileVisitor visitor = new WriterFileVisitor(Files.newBufferedWriter(outPath))) {
                handleInput(inPath, visitor);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        } catch (InvalidPathException | NullPointerException e) {
            System.err.println(e.getMessage());
        }
    }
}
