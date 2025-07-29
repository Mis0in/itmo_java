package info.kgeorgiy.ja.boin.implementor.parts.lines;

import info.kgeorgiy.ja.boin.implementor.parts.AbstractClassPart;

import java.io.IOException;
import java.io.Writer;

/**
 * An abstract base class representing a line of code. It extends {@link AbstractClassPart}
 * and implements the {@link Line} interface, providing functionality for writing a single line
 * of code to a file.
 */
public abstract class AbstractLine extends AbstractClassPart implements Line {
    /**
     * The content of the line.
     */
    final String line;

    /**
     * Constructs a new instance of Line
     *
     * @param line the content of the line
     */
    public AbstractLine(String line) {
        this.line = line;
    }

    /**
     * writes a single line to a file
     * @param writer the writer to which the content will be written
     * @throws IOException on error
     */
    @Override
    public void writeTo(Writer writer) throws IOException {
        writeFormatted(writer, line);
    }
}
