package team.magic.flute.hercules.common.classloader;


import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LocalJarInputStream extends InputStream {

    private JarFile jarFile;
    private InputStream inputStream;

    public LocalJarInputStream(LocalJarURLConnection localJarURLConnection) throws IOException {
        this.jarFile = localJarURLConnection.getJarFile();
        JarEntry jarEntry = jarFile.getJarEntry(localJarURLConnection.getEntryName());
        if (jarEntry == null || jarEntry.isDirectory()) {
            throw new IOException(String.format("Resource not found:%s", localJarURLConnection.getURL()));
        }
        this.inputStream = jarFile.getInputStream(jarEntry);
    }

    @Override
    public int read() throws IOException {
        return this.inputStream.read();
    }

    public void close() throws IOException {
        IOException exception = null;
        try{
            if(inputStream!=null){
                inputStream.close();
            }
        }catch (IOException e){
            exception = e;
        }
        try{
            if(jarFile!=null){
                jarFile.close();
            }
        } catch (IOException e) {
            exception = e;
        }
        if(exception!=null){
            throw exception;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
    }
}
