package info.kgeorgiy.ja.boin.implementor.parts.bodies;

import info.kgeorgiy.ja.boin.implementor.parts.AbstractClassPart;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents the body of a constructor that calls the superclass constructor with the provided arguments.
 */
public class ConstructorSuperBody extends AbstractClassPart implements Body {
    /**
     * The argument types for the superclass constructor.
     */
    Class<?>[] arguments;

    /**
     * Constructs a new instance of constructor body, which contains
     * body with only one call to super with given arguments
     *
     * @param args the argument types for the superclass constructor
     */
    public ConstructorSuperBody(Class<?>[] args) {
        arguments = args;
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        writeFormatted(writer, String.format("super(%s);", argsToString(arguments, (c) -> "")));
    }
}
