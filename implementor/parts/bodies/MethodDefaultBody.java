package info.kgeorgiy.ja.boin.implementor.parts.bodies;

import info.kgeorgiy.ja.boin.implementor.parts.AbstractClassPart;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents the body of a method that returns a default value based on the method's return type.
 */
public class MethodDefaultBody extends AbstractClassPart implements Body {
    /**
     * The return type of the method.
     */
    Class<?> returnType;

    /**
     * Constructs a new instance of MethodDefaultBody which represents body
     * with only one return of default type
     *
     * @param returnType the return type of the method
     */
    public MethodDefaultBody(Class<?> returnType) {
        this.returnType = returnType;
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        writeFormatted(writer, String.format("return %s;", typeDefault(returnType)));
    }
}

