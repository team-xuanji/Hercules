package team.magic.flute.hercules.common.classloader;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class LocalJarURLStreamHandlerFactory implements URLStreamHandlerFactory {
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return new LocalJarURLStreamHandler();
    }
}
