package info.kgeorgiy.ja.boin.implementor.parts;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * An abstract base class for implementing {@link ClassPart} that provides common functionality
 * for formatting and writing class parts to a file.
 */
public abstract class AbstractClassPart implements ClassPart {
    /**
     * A static counter to track the current indentation level for formatting.
     */
    protected static int formatCount = 0;
    /**
     * The number of spaces used for each indentation level.
     */
    protected final int formatStep = 4;

    /**
     * Create instance of class part
     */
    public AbstractClassPart(){}

    /**
     * Writes a line of text to the writer with the spaces based on the current {@link #formatCount}.
     *
     * @param writer writer into which the line will be written
     * @param line   line of text to write
     * @throws IOException if an I/O error occurs during writing
     */
    protected void writeFormatted(Writer writer, String line) throws IOException {
        writer.write(" ".repeat(formatCount));
        writer.write(line);
        writer.write(System.lineSeparator());
    }

    /**
     * Converts an array of classes into a comma-separated string using the provided mapping function.
     *
     * @param args       array of classes to convert
     * @param classToStr a function that maps a class to its string representation
     * @return a comma-separated string of the mapped class names
     */
    public String classesToString(Class<?>[] args, Function<Class<?>, String> classToStr) {
        return Arrays.stream(args)
                .map(classToStr)
                .reduce((arg1, arg2) -> arg1 + ", " + arg2)
                .orElse("");
    }

    /**
     * Converts an array of classes into a comma-separated string of arguments, appending a unique identifier
     * to each argument name (e.g., "arg0", "arg1", etc.).
     *
     * @param args       the array of classes to convert
     * @param classToStr a function that maps a class to its string representation
     * @return a comma-separated string of the mapped class names with argument identifiers
     */
    public String argsToString(Class<?>[] args, Function<Class<?>, String> classToStr) {
        AtomicInteger argNumber = new AtomicInteger();
        return classesToString(args,
                (clazz) -> classToStr.apply(clazz) + "arg" + argNumber.getAndIncrement()
        );
    }

    /**
     * Default value for a given type, which is used in method implementations.
     *
     * @param type token which default value should be returned
     * @return string representing the default value for the type (e.g., "false" for boolean, "0" for int, "null" for objects)
     */
    protected String typeDefault(Class<?> type) {
        if (type == void.class) {
            return "";
        } else if (type == boolean.class) {
            return "false";
        } else if (type.isPrimitive()) {
            return "0";
        }
        return "null";
    }
}
