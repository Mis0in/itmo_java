package info.kgeorgiy.ja.boin.implementor.parts;

import java.lang.reflect.Modifier;

/**
 * Represents the privacy levels (access modifiers) for classes, methods, and fields.
 * This enum provides a mapping between Java access modifiers and their string representations.
 */
public enum PrivacyLevel {
    /**
     * Representation of public level of privacy
     */
    PUBLIC("public"),

    /**
     * Representation of private level of privacy
     */
    PRIVATE("private"),
    /**
     * Representation of protected level of privacy
     */
    PROTECTED("protected"),
    /**
     * Representation of package private level of privacy
     */
    PACKAGE("");

    /**
     * The string representation of the privacy level.
     */
    final String level;

    /**
     * creates a privacy level
     *
     * @param lvl string representation of the privacy level
     */
    PrivacyLevel(String lvl) {
        level = lvl;
    }

    /**
     * Returns the string representation of the privacy level.
     *
     * @return the string representation of the privacy level
     */
    @Override
    public String toString() {
        return level;
    }

    /**
     * Determines the privacy level based on the provided modifier flags.
     *
     * @param mod the modifier flags (e.g., from {@link java.lang.reflect.Method#getModifiers()})
     * @return `PrivacyLevel` (e.g., `PUBLIC`, `PRIVATE`, `PROTECTED`, or `PACKAGE`)
     */
    public static PrivacyLevel getPrivacy(int mod) {
        PrivacyLevel privacy;

        if (Modifier.isPrivate(mod)) {
            privacy = PrivacyLevel.PRIVATE;
        } else if (Modifier.isProtected(mod)) {
            privacy = PrivacyLevel.PROTECTED;
        } else if (Modifier.isPublic(mod)) {
            privacy = PrivacyLevel.PUBLIC;
        } else {
            privacy = PrivacyLevel.PACKAGE;
        }

        return privacy;
    }
}
