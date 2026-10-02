package team.magic.flute.hercules.common.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

public class LocalJarURLConnection extends JarURLConnection {

    /**
     * Creates the new JarURLConnection to the specified URL.
     *
     * @param url the URL
     * @throws MalformedURLException if no legal protocol
     *                               could be found in a specification string or the
     *                               string could not be parsed.
     */
    protected LocalJarURLConnection(URL url) throws MalformedURLException {
        super(url);
    }

    @Override
    public JarFile getJarFile() throws IOException {
        URL jarFileURL = this.getJarFileURL();
        return new JarFile(jarFileURL.getFile());
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new LocalJarInputStream(this);
    }

}
