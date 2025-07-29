package info.kgeorgiy.ja.boin.implementor.parts.scopes;

import info.kgeorgiy.ja.boin.implementor.parts.PrivacyLevel;
import info.kgeorgiy.ja.boin.implementor.parts.AbstractClassPart;

import java.io.IOException;
import java.io.Writer;

/**
 * An abstract class representing a scope (e.g., class, method, or constructor). It extends
 * {@link AbstractClassPart} and implements the {@link Scope} interface, providing common
 * functionality for writing scopes to a file.
 */
public abstract class AbstractScope extends AbstractClassPart implements Scope {
    /**
     * The name of the block
     */
    protected String blockName;

    /**
     * The privacy level of the scope
     */
    protected PrivacyLevel privacy;

    /**
     * Creates instance of scope
     */
    public AbstractScope(){}

    /**
     * Writes the scope content itself, bracers will be written automatically
     *
     * @param writer the writer to which the content will be written
     * @throws IOException if an I/O error occurs while writing
     */
    protected abstract void writeToImpl(Writer writer) throws IOException;

    /**
     * Returns the signature of the scope
     *
     * @return the signature of the scope as a string
     */
    protected abstract String getSignature();

    @Override
    public final void writeTo(Writer writer) throws IOException {
        writeFormatted(writer, getSignature() + " {");

        formatCount += formatStep;
        writeToImpl(writer);
        formatCount -= formatStep;

        writeFormatted(writer, "}");
        writeFormatted(writer, "");
    }

    // --building utils--

    /**
     * Builds a signature string for a method or constructor based on its privacy level, return type,
     * name, arguments, and exceptions.
     *
     * @param privacy    privacy level of the method or constructor
     * @param returnType return type of the method (null for constructors)
     * @param name       name of the method or constructor
     * @param args       parameter types of the method or constructor
     * @param exceptions exception types thrown by the method or constructor
     * @return constructed signature as a string
     */
    public String buildSignature(PrivacyLevel privacy, Class<?> returnType, String name, Class<?>[] args, Class<?>[] exceptions) {
        String returnName = returnType != null ? returnType.getCanonicalName() : "";
        String signature = String.format("%s %s%s(%s)",
                privacy,
                returnName,
                name,
                argsToString(args, (arg) -> arg.getCanonicalName() + " ")
        );

        if (exceptions.length != 0) {
            signature += " throws " + classesToString(exceptions, Class::getCanonicalName);
        }
        return signature;
    }
}
