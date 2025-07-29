package info.kgeorgiy.ja.boin.implementor.parts;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents a part of a class (e.g., a method, constructor, or field) that can be written to a file.
 * Implementing classes must provide a method to write their content to a {@link Writer}.
 */
public interface ClassPart {
    /**
     * Writes the content of this class part to the provided {@link Writer}.
     *
     * @param writer the writer to which the content will be written
     * @throws IOException if an I/O error occurs while writing
     */
    void writeTo(Writer writer) throws IOException;
}
