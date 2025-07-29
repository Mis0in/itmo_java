package info.kgeorgiy.ja.boin.implementor.parts.scopes;

import info.kgeorgiy.ja.boin.implementor.parts.PrivacyLevel;
import info.kgeorgiy.ja.boin.implementor.parts.ClassPart;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Represents a class scope, including its name, privacy level, superclass/interface, and class parts
 * (e.g., methods, constructors).
 */
public class ClassScope extends AbstractScope {
    /**
     * List containing elements which are representing body of class
     */
    private final List<ClassPart> scopeParts;
    /**
     * Class token that will be implemented (extended) by our class signature
     */
    private final Class<?> implement;

    /**
     * creates new instance of class scope
     *
     * @param privacy class privacy level
     * @param name class name
     * @param impl class token that will be implemented(extended) by out class
     * @param parts class token that will be implemented (extended) by our class signature
     */
    public ClassScope(PrivacyLevel privacy, String name, Class<?> impl, List<ClassPart> parts) {
        this.privacy = privacy;
        blockName = name;
        implement = impl;
        scopeParts = parts;
    }

    @Override
    protected void writeToImpl(Writer writer) throws IOException {
        for (ClassPart part : scopeParts) {
            part.writeTo(writer);
        }
    }

    @Override
    protected String getSignature() {
        StringBuilder className = new StringBuilder(privacy + " class " + blockName + " ");
        String impl = implement.isInterface() ? "implements " : "extends ";
        className.append(impl).append(implement.getCanonicalName());

        return className.toString();
    }
}
