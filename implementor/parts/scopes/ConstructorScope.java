package info.kgeorgiy.ja.boin.implementor.parts.scopes;

import info.kgeorgiy.ja.boin.implementor.parts.bodies.ConstructorSuperBody;

import java.lang.reflect.Constructor;

import static info.kgeorgiy.ja.boin.implementor.parts.PrivacyLevel.getPrivacy;

/**
 * Represents a constructor scope, including its privacy level, name, parameters, and body.
 */
public class ConstructorScope extends AbstractMethodScope {
    /**
     * constructor itself
     */
    private final Constructor<?> con;
    /**
     * Create new instance of constructor scope.
     *
     * @param con       constructor being represented
     * @param className name of the class containing the constructor
     * @param body      body of the constructor
     */
    public ConstructorScope(Constructor<?> con, String className, ConstructorSuperBody body) {
        privacy = getPrivacy(con.getModifiers());
        blockName = className;
        this.con = con;
        this.body = body;
    }

    @Override
    protected String getSignature() {
        return buildSignature(getPrivacy(con.getModifiers()), null, blockName, con.getParameterTypes(), con.getExceptionTypes());
    }
}
