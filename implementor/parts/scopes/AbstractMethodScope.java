package info.kgeorgiy.ja.boin.implementor.parts.scopes;

import info.kgeorgiy.ja.boin.implementor.parts.bodies.Body;

import java.io.IOException;
import java.io.Writer;

/**
 * An abstract class representing a method or constructor scope. It extends {@link AbstractScope}
 * and provides functionality for writing the body of a method or constructor.
 */
public abstract class AbstractMethodScope extends AbstractScope {
    /**
     * The body of the method or constructor.
     */
    Body body;


    /**
     * Creates instance of method-like scope
     */
    public AbstractMethodScope(){}

    @Override
    protected void writeToImpl(Writer writer) throws IOException {
        body.writeTo(writer);
    }
}
