package team.magic.flute.hercules.executor.download;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Downloader Interface
 *
 * <p>This interface defines the contract for downloading files from various sources.
 * It provides a pluggable architecture for supporting different protocols and storage systems.
 *
 * <p>Implementations of this interface handle protocol-specific download logic while
 * providing a consistent API for file retrieval operations. The interface supports
 * downloading files from remote locations to local file system destinations.
 *
 * <p>Common use cases include:
 * <ul>
 *   <li>Downloading plugin JAR files from HTTP/HTTPS servers</li>
 *   <li>Retrieving resources from cloud storage services</li>
 *   <li>Fetching configuration files and data assets</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface Downloader {

    /**
     * Get the URI schema that this downloader supports.
     *
     * <p>Returns the protocol scheme (e.g., "http", "https", "ftp") that this
     * downloader implementation can handle. This is used by the DownloaderManager
     * to route download requests to the appropriate implementation.
     *
     * @return the URI schema supported by this downloader
     */
    String getSchema();

    /**
     * Download a file from a remote URI to a local directory.
     *
     * <p>This is a convenience method that creates a File object from the destination
     * directory string and delegates to the main download method.
     *
     * @param srcURI the source URI to download from
     * @param dstDir the destination directory path as a string
     * @param filename the name for the downloaded file
     * @return the downloaded local file
     * @throws IOException if the download operation fails
     */
    default File downloadToLocal(URI srcURI, String dstDir, String filename) throws IOException {
        return downloadToLocal(srcURI, new File(dstDir), filename);
    }

    /**
     * Download a file from a remote URI to a local directory.
     *
     * <p>This method performs the actual download operation, retrieving the file
     * from the specified source URI and saving it to the destination directory
     * with the given filename.
     *
     * @param srcURI the source URI to download from
     * @param dstDir the destination directory as a File object
     * @param filename the name for the downloaded file
     * @return the downloaded local file
     * @throws IOException if the download operation fails due to network issues,
     *                     file system errors, or other I/O problems
     */
    File downloadToLocal(URI srcURI, File dstDir, String filename) throws IOException;
}
