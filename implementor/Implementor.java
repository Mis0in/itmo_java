package info.kgeorgiy.ja.boin.implementor;

import info.kgeorgiy.ja.boin.implementor.parts.ClassPart;
import info.kgeorgiy.ja.boin.implementor.parts.PrivacyLevel;
import info.kgeorgiy.ja.boin.implementor.parts.scopes.ClassScope;
import info.kgeorgiy.ja.boin.implementor.parts.scopes.ConstructorScope;
import info.kgeorgiy.ja.boin.implementor.parts.scopes.MethodScope;
import info.kgeorgiy.ja.boin.implementor.parts.bodies.ConstructorSuperBody;
import info.kgeorgiy.ja.boin.implementor.parts.bodies.MethodDefaultBody;
import info.kgeorgiy.ja.boin.implementor.parts.lines.AnnotationLine;
import info.kgeorgiy.ja.boin.implementor.parts.lines.PackageLine;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;


/**
 * Class that contains methods for creating java code,
 * that implements other given java class and creating
 * default implementation methods for another.
 * (such that they are returning default values)
 */
public class Implementor implements JarImpler {

    /**
     * Creates new instances of class Implementor
     */
    public Implementor(){}

    /**
     * Prepares the file system path by combining the root directory and the package name.
     *
     * @param root        the root directory where the package structure will be created
     * @param packageName the name of the package, typically in dot notation (e.g., "com.example.package")
     * @return a string representing the full path, including the package structure
     */
    private String preparePath(Path root, String packageName) {
        return root + File.separator + packageName.replace('.', File.separatorChar);
    }


    /**
     * Creates a file path for the Java implementation file based on the provided class name, package name, and root directory.
     * This method ensures that the necessary directories are created and returns the full path to the Java file.
     *
     * @param name        name of the class to be implemented (without the ".java" extension)
     * @param packageName name of the package where the class resides
     * @param root        root directory where the package structure and file will be created
     * @return full path to the Java implementation file
     * @throws ImplerException if an I/O error occurs while creating the directories
     */
    private Path createPath(String name, String packageName, Path root) throws ImplerException {
        try {
            String directories = preparePath(root, packageName);
            String className = name + ".java";

            Files.createDirectories(Path.of(directories));
            return Path.of(directories + File.separator + className);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Constructs a list of {@link ClassPart} objects which contains methods that can be overridden
     * This includes public and protected methods from the given class and its superclasses
     * that are abstract.
     *
     * @param token class token for which methods need to be overridden
     * @return list of {@link ClassPart} objects representing methods and their annotations
     */
    private List<ClassPart> getMethodsToOverride(Class<?> token) {
        List<ClassPart> toOverride = new ArrayList<>();
        //add all public methods
        List<Method> allMethods = new ArrayList<>(List.of(token.getMethods()));

        //add all protected methods
        for (;token != null; token = token.getSuperclass()) {
            allMethods.addAll(Stream.of(token.getDeclaredMethods()).filter(
                    method -> Modifier.isProtected(method.getModifiers())).toList());
        }

        //build annotations and methods from collected previously
        for (Method m : allMethods) {
            int mod = m.getModifiers();
            if (Modifier.isAbstract(mod)) {
                //add annotations
                toOverride.addAll(Arrays.stream(m.getAnnotations()).map((annotation -> new AnnotationLine(annotation.toString()))).toList());
                toOverride.add(new AnnotationLine("@Override"));

                toOverride.add(new MethodScope(m, new MethodDefaultBody(m.getReturnType())));
            }
        }
        return toOverride;
    }

    /**
     * Constructs a list of {@link ClassPart} objects which contains only public and protected constructors of the given class.
     * If no such constructors are found and the token is not an interface,
     * an {@link ImplerException} is thrown.
     *
     * @param token     class for which constructors are to be collected
     * @param className name of the implementation class to which the constructors will belong
     * @return a list of {@link ClassPart} objects representing the constructors
     * @throws ImplerException if no public or protected constructors are found and the token is not an interface
     */
    private static List<ClassPart> getConstructors(Class<?> token, String className) throws ImplerException {
        var constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(con -> !Modifier.isPrivate(con.getModifiers()))
                .<ClassPart>map(con -> new ConstructorScope(con, className, new ConstructorSuperBody(con.getParameterTypes())))
                .toList();
        if (constructors.isEmpty() && !token.isInterface()) {
            throw new ImplerException("No public/protected constructors of class was found");
        }
        return constructors;
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        ensureClassToken(token);
        String className = token.getSimpleName() + "Impl";
        Path classPath = createPath(className, token.getPackageName(), root);

        try (Writer out = Files.newBufferedWriter(classPath)) {
            if (!token.getPackageName().isEmpty()) {
                new PackageLine(token.getPackageName()).writeTo(out);
            }

            //Creating a tree structure of all methods and constructors
            //ClassBlock will be a root of a tree
            //Then recursively walking down, writing class parts to a file, using writeTo(writer)
            ClassPart classBlock = new ClassScope(PrivacyLevel.PUBLIC, className, token,
                    Stream.concat(
                            getConstructors(token, className).stream(),
                            getMethodsToOverride(token).stream()
                    ).toList()
            );

            classBlock.writeTo(out);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * This method ensures that class token can be implemented
     *
     * @param token token that will be checked
     * @throws ImplerException if class could not be implemented
     */
    private void ensureClassToken(Class<?> token) throws ImplerException {
        int mod = token.getModifiers();
        if (token.isPrimitive()
                || token.isEnum()
                || token.isSealed()
                || Modifier.isPrivate(mod)
                || Modifier.isFinal(mod)
                || token.equals(Enum.class)
                || token.equals(Record.class)
        ) {
            throw new ImplerException("Unable to extend given class");
        }
    }

    /**
     * Compile files with given dependencies and encoding
     *
     * @param files files to compile
     * @param dependencies dependencies to add to every file
     * @param charset encoding charset to use in compilation
     * @throws ImplerException on error
     */
    public static void compile(
            final List<Path> files,
            final List<Class<?>> dependencies,
            final Charset charset
    ) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = getClassPath(dependencies).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        final String[] args = Stream.concat(
                Stream.of("-cp", classpath, "-encoding", charset.name()),
                files.stream().map(Path::toString)
        ).toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Unable to compile class");
        }
    }

    /**
     * Converts a list of classes into a list of paths to JAR files or directories
     * from which these classes were loaded.
     *
     * @param dependencies list of classes for which to search source paths
     * @return an immutable list of paths to JAR files or directories containing the specified classes
     * @throws AssertionError if a {@link URISyntaxException} occurs during URL-to-URI conversion
     *         (should not occur under normal circumstances)
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
                    } catch (final URISyntaxException e) {
                        throw new AssertionError(e);
                    }
                })
                .toList();
    }

    /**
     * Creating java code, depends on given parameters
     *
     * @param args can be either:
     *             1. arg[0] path to java file, which implementations should be created
     *             2. args[0] -jar
     *                arg[1]  same as in first case
     *                args[2]  name of jar file
     *
     * @throws ClassNotFoundException if class path is invalid
     * @throws ImplerException on error during creating java code
     */
    public static void main(String[] args) throws ClassNotFoundException, ImplerException {
        Implementor implementor = new Implementor();
        Path pwd = Path.of(System.getProperty("user.dir"));
        if (args.length == 1) {
            implementor.implement(Class.forName(args[0]), pwd);
        } else if (args.length == 3) {
            implementor.implementJar(Class.forName(args[1]), pwd);
        } else {
            throw new ImplerException("Exactly one argument expected");
        }
    }

    @Override
    public void implementJar(Class<?> token, Path path) throws ImplerException {
        final Path workingDir = path.getParent();
        implement(token, workingDir);

        String className = token.getSimpleName() + "Impl";
        Path classPath = Path.of(preparePath(workingDir, token.getPackageName()) + File.separator + className);
        Path uncompiled = Path.of(classPath + ".java");
        Path compiled = Path.of(classPath + ".class");
        compile(List.of(uncompiled), List.of(token), StandardCharsets.UTF_8);

        try {
            JarOutputStream jout = new JarOutputStream(Files.newOutputStream(path));
            try {
                String correctPath = token.getPackageName().replace('.', '/') + '/' + className + ".class";
                jout.putNextEntry(new ZipEntry(correctPath));
                Files.copy(compiled,jout);
            } finally {
                jout.finish();
                jout.close();
            }
            Files.delete(compiled);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage(), e);
        }
    }
}
