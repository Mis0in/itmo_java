package info.kgeorgiy.ja.boin.implementor.parts.lines;
/**
 * Represents a package declaration line in the generated code.
 */
public class PackageLine extends AbstractLine {
    /**
     * creates a line of package
     *
     * @param packageName the name of the package
     */
    public PackageLine(String packageName) {
        super("package " + packageName + ";");
    }
}
