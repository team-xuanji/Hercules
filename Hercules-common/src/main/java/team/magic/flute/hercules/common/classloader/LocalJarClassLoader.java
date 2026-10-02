package team.magic.flute.hercules.common.classloader;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Local JAR File Class Loader
 *
 * <p>A custom class loader implementation that loads classes and resources directly from
 * a local JAR file. This class loader is designed for dynamic plugin loading in the
 * Hercules system, allowing plugins to be loaded at runtime from JAR files.
 *
 * <p>Key features:
 * <ul>
 *   <li>Direct JAR file loading without requiring classpath modifications</li>
 *   <li>Parent delegation support for accessing executor classes</li>
 *   <li>Resource caching for improved performance</li>
 *   <li>Optional automatic file cleanup on close</li>
 *   <li>Thread-safe concurrent access to cached resources</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * File pluginJar = new File("/path/to/plugin.jar");
 * try (LocalJarClassLoader loader = new LocalJarClassLoader(pluginJar, getClass().getClassLoader())) {
 *     Class<?> pluginClass = loader.loadClass("com.example.MyPlugin");
 *     Object plugin = pluginClass.getDeclaredConstructor().newInstance();
 *     // Use the plugin...
 * }
 * }</pre>
 *
 * <p>The class loader maintains internal caches for both class files and resources to
 * improve performance during repeated access. It also supports automatic cleanup of
 * temporary JAR files when configured to do so.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class LocalJarClassLoader extends ClassLoader implements Closeable {

    /**
     * URL stream handler for jar: protocol URLs.
     */
    private static final LocalJarURLStreamHandler urlStreamHandler = new LocalJarURLStreamHandler();

    /**
     * The JAR file being loaded by this class loader.
     */
    private File file;

    /**
     * Whether to delete the JAR file when this class loader is closed.
     * Useful for temporary plugin files.
     */
    private boolean deleteFileOnClose = false;

    /**
     * Cache mapping class names to their file paths within the JAR.
     * Improves performance by avoiding repeated JAR scanning.
     */
    private ConcurrentHashMap<String, String> classFileCache = new ConcurrentHashMap<>();

    /**
     * Cache mapping resource names to their URLs.
     * Improves performance for repeated resource access.
     */
    private ConcurrentHashMap<String, URL> resourceUrlCache = new ConcurrentHashMap<>();

    /**
     * Create a new LocalJarClassLoader without automatic file deletion.
     *
     * @param file the JAR file to load classes from
     * @param parent the parent class loader for delegation
     * @throws IOException if the JAR file cannot be read or is invalid
     */
    public LocalJarClassLoader(File file, ClassLoader parent) throws IOException {
        this(file, parent, false);
    }

    /**
     * Create a new LocalJarClassLoader with configurable file deletion.
     *
     * <p>This constructor allows you to specify whether the JAR file should be
     * automatically deleted when the class loader is closed. This is useful
     * for temporary plugin files that should be cleaned up after use.
     *
     * @param file the JAR file to load classes from
     * @param parent the parent class loader for delegation
     * @param deleteFileOnClose whether to delete the JAR file when closed
     * @throws IOException if the JAR file cannot be read or is invalid
     */
    public LocalJarClassLoader(File file, ClassLoader parent, boolean deleteFileOnClose) throws IOException {
        super(parent);
        this.file = file;
        this.deleteFileOnClose = deleteFileOnClose;

        try (JarFile jarFile = new JarFile(file);) {

            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {

                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    continue;
                }

                boolean isClass = entryName.endsWith(".class");
                if (isClass) {
                    String className = entryName.replaceAll("\\\\", ".").replaceAll("/", ".");
                    classFileCache.put(className, entryName);
                } else {
                    String spec = String.format("jar:file:%s!/%s", jarFile.getName(), entryName);
                    URL jarFileUrl = new URL(null, spec, urlStreamHandler);
                    resourceUrlCache.put(entryName, jarFileUrl);
                }


            }


        }

    }

    @Override
    public URL getResource(String name) {
        URL url = resourceUrlCache.get(name);
        return url;
    }

    protected Enumeration<URL> findResources(String name) throws IOException {
        URL url = resourceUrlCache.get(name);
        if (url == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(Collections.singletonList(url));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classData = loadClassData(name);
        if (classData == null) {
            throw new ClassNotFoundException("Class " + name + " not found");
        }
        return defineClass(name, classData, 0, classData.length);
    }

    private byte[] loadClassData(String className) {
        String path = classFileCache.get(className + ".class");
        if (path == null) {
            return null;
        }

        return this.getFileAllBytes(path);
    }

    private byte[] getFileAllBytes(String path) {
        try (JarFile jarFile = new JarFile(file);) {

            JarEntry jarEntry = jarFile.getJarEntry(path);
            if (jarEntry == null || jarEntry.isDirectory()) {
                return null;
            }

            try (InputStream inputStream = jarFile.getInputStream(jarEntry);) {
                byte[] allBytes = new byte[(int) jarEntry.getSize()];
                int position = 0;

                byte[] buffer = new byte[1024];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    System.arraycopy(buffer, 0, allBytes, position, bytesRead);
                    position += bytesRead;
                }

                return allBytes;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (this.classFileCache != null) {
            this.classFileCache.clear();
        }
        if (this.resourceUrlCache != null) {
            this.resourceUrlCache.clear();
        }
        if (this.file != null && this.file.exists() && this.deleteFileOnClose) {
            this.file.delete();
            this.file = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.close();
    }
}
