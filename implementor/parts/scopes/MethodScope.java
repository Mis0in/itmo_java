package info.kgeorgiy.ja.boin.implementor.parts.scopes;

import info.kgeorgiy.ja.boin.implementor.parts.PrivacyLevel;
import info.kgeorgiy.ja.boin.implementor.parts.bodies.MethodDefaultBody;

import java.lang.reflect.Method;

/**
 * Represents a method scope, including its privacy level, return type, name, parameters, exceptions, and body.
 */
public class MethodScope extends AbstractMethodScope {
    /**
     * Method's info which will be used to create method (for example return type)
     *
     */
    private final Method method;

    /**
     * Constructs a new method scope.
     *
     * @param m    method being represented
     * @param body body of the method
     */
    public MethodScope(Method m, MethodDefaultBody body) {
        privacy = PrivacyLevel.getPrivacy(m.getModifiers());
        method = m;
        this.body = body;
    }

    @Override
    protected String getSignature() {
        return buildSignature(privacy, method.getReturnType(), " " + method.getName(), method.getParameterTypes(), method.getExceptionTypes());
    }
}
