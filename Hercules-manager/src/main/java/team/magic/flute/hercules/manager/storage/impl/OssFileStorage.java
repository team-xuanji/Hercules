package team.magic.flute.hercules.manager.storage.impl;

import com.aliyun.oss.*;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import team.magic.flute.hercules.manager.exception.PluginRegisterFailedException;
import team.magic.flute.hercules.manager.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("OSS_FILE_STORAGE")
public class OssFileStorage implements FileStorage {

    private Cache<String,URL> ossHttpUrlCache;

    @Value("${file-storage.oss.endpoint}")
    private String endPoint;
    @Value("${file-storage.oss.bucket}")
    private String bucket;
    @Value("${file-storage.oss.access}")
    private String accessKeyId;
    @Value("${file-storage.oss.secret}")
    private String accessKeySecret;
    @Value("${file-storage.oss.root-path}")
    private String rootPath;
    @Value("${file-storage.oss.support-http-download}")
    private boolean supportHttpDownload;

    private final static String OSS_URL_TEMPLATE = "OSS://{bucket}.{endpoint}/{path}";

    private static final String OSS_SEPARATOR = "/";
    private OSS ossClient;

    @PostConstruct
    private void initOssClient(){
        final ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        ossClient = new OSSClientBuilder()
                .build(endPoint, accessKeyId, accessKeySecret, clientBuilderConfiguration);
        ossHttpUrlCache = Caffeine.newBuilder()
                // Set expiration after a fixed time since last write or access
                // Since the currently generated OSS URL is valid for 10 minutes, set it to 8 minutes here to create a time buffer.
                .expireAfterWrite(8, TimeUnit.MINUTES)
                // Initial cache space size
                .initialCapacity(10)
                // Maximum number of cache entries
                .maximumSize(20000)
                .build(this::getOssUrl);
    }


    @Override
    public PluginResourceInfo getPlugin(HerculesPluginInfoPo plugin) {
        try{
            PluginResourceInfo pluginResourceInfoDescriptor = new PluginResourceInfo();
            if(plugin==null){
                return pluginResourceInfoDescriptor;
            }
            pluginResourceInfoDescriptor.setPluginGroup(plugin.getPluginGroup());
            pluginResourceInfoDescriptor.setVersionId(plugin.getRevision());
            Set<String> resources = plugin.getResources();
            if(resources==null){
                return pluginResourceInfoDescriptor;
            }
            for (String resource : resources) {
                String fileName = resource.substring(resource.lastIndexOf(OSS_SEPARATOR)+1);
                URL fileLocation = parse2HttpUrls(resource);
                pluginResourceInfoDescriptor.addResource(fileName,fileLocation.toURI());
            }
            pluginResourceInfoDescriptor.setCanDownloadByHttp(supportHttpDownload);
            return pluginResourceInfoDescriptor;
        }catch (Exception e){
            throw new PluginRegisterFailedException("Plugin registration failed!",e);
        }
    }

    @Override
    public Set<String> getUrls(String pluginGroup, MultipartFile[] files) {
        Set<String> result = new HashSet<>();
        for (MultipartFile file : files) {
            String path = getOssUrl(pluginGroup,file);
            result.add(path);
        }
        return result;
    }

    private String getOssUrl(String pluginName, MultipartFile file){
        String fullPath = getFullRemotePath(pluginName,file);
        return  OSS_URL_TEMPLATE.replace("{bucket}",bucket)
                .replace("{endpoint}",endPoint)
                .replace("{path}",fullPath);
    }
    private String getFullRemotePath(String pluginName, MultipartFile file){
        String path = getPluginStorageDirPath(pluginName)+file.getOriginalFilename();
        if (StringUtils.startsWith(path, OSS_SEPARATOR)) {
            return StringUtils.replaceOnce(path, OSS_SEPARATOR, "");
        }
        return path;
    }

    private String getPluginStorageDirPath(String pluginGroup){
        if(rootPath.endsWith(OSS_SEPARATOR)){
            return rootPath+DigestUtils.md5Hex(pluginGroup.trim())+OSS_SEPARATOR;
        }else{
            return rootPath+OSS_SEPARATOR+DigestUtils.md5Hex(pluginGroup.trim())+OSS_SEPARATOR;
        }

    }


    @Override
    public void download(String pluginGroup, String fileName, HttpServletResponse response) {
        throw new UnsupportedOperationException("OSS file download is currently not supported");
    }

    @Override
    public void download2LocalFile(String ossKey, File destFile) {
        GetObjectRequest request = new GetObjectRequest(bucket, getOssKey(ossKey));
        try {
            ossClient.getObject(request, destFile);
            log.info("Successfully downloaded OSS file, OssFile={}, target file={}", ossKey, destFile);
        } catch (OSSException | ClientException e) {
            log.error("Failed to download OSS file, OssFile={}, target file={}", ossKey, destFile, e);
            throw e;
        }
    }

    @Override
    public InputStream getDownloadStream(String ossKey) {
        GetObjectRequest request = new GetObjectRequest(bucket, getOssKey(ossKey));
        return ossClient.getObject(request).getObjectContent();
    }

    @Override
    public void upload(String pluginGroup, MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if(StringUtils.isNotBlank(fileName)){
                String remotePath = getFullRemotePath(pluginGroup,file);
                upload(file.getInputStream(),remotePath,true);
            }
        }
    }

    @Override
    public void delete(String pluginGroup) {
        String remoteRootPath = getPluginStorageDirPath(pluginGroup);
        deleteDir(remoteRootPath);
    }



    @Override
    public URL parse2HttpUrls(String originalUrl) {
        String key = originalUrl.replaceAll("(oss|OSS)://.*?/","");
//        return getOssUrl(key);
        if(ossHttpUrlCache.getIfPresent(key)==null){
            synchronized (this){
                if(ossHttpUrlCache.getIfPresent(key)==null){
                    ossHttpUrlCache.put(key,getOssUrl(key));
                }
            }
        }
        return ossHttpUrlCache.getIfPresent(key);
    }


    public void upload(Path local, String remotePath) {
        final File localFile = local.toFile();
        final PutObjectRequest request = new PutObjectRequest(bucket, getOssKey(remotePath), localFile);
        request.setMetadata(getOssDefaultMetadata(false));
        putObject(request);
    }

    private void putObject(PutObjectRequest request) {
        try {
            ossClient.putObject(request);
        } catch (OSSException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get OSS target file stream
     *
     * @param remotePath full path
     * @return InputStream
     */
    public InputStream getInputStream(String remotePath) {
        // ossObject contains the bucket name, file name, file metadata, and an input stream.
        OSSObject ossObject = ossClient.getObject(bucket, getOssKey(remotePath));
        return ossObject.getObjectContent();
    }

    public void upload(InputStream inputStream, String remotePath, boolean overwrite) {
        final PutObjectRequest request = new PutObjectRequest(bucket, getOssKey(remotePath), inputStream);
        request.setMetadata(getOssDefaultMetadata(overwrite));
        putObject(request);
    }

    private String getOssKey(String path) {
        if (StringUtils.startsWith(path, OSS_SEPARATOR)) {
            return StringUtils.replaceOnce(path, OSS_SEPARATOR, "");
        }
        return path;
    }

    private ObjectMetadata getOssDefaultMetadata(boolean overwrite) {
        ObjectMetadata metadata = new ObjectMetadata();
        // Prohibit file overwrite
        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
        if (!overwrite) {
            metadata.setHeader("x-oss-forbid-overwrite", "true");
        }
        return metadata;
    }


    public void deleteByPath(String remotePath) {
        try {
            ossClient.deleteObject(bucket, remotePath);
            log.info("Successfully deleted [{}]Bucket, remotePath[{}]", bucket, remotePath);
        } catch (OSSException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    // Code copied from official website, unchanged
    public void deleteDir(String dirPath) {
        if(!dirPath.endsWith(OSS_SEPARATOR)){
            throw new IllegalArgumentException("When deleting OSS directory, the directory path must end with /");
        }
        try {
            // List and delete all files containing the specified prefix.
            String nextMarker = null;
            ObjectListing objectListing = null;
            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucket)
                        .withPrefix(dirPath)
                        .withMarker(nextMarker);

                objectListing = ossClient.listObjects(listObjectsRequest);
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    List<String> keys = new ArrayList<String>();
                    for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                        keys.add(s.getKey());
                    }
                    DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket).withKeys(keys).withEncodingType("url");
                    DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(deleteObjectsRequest);
                    List<String> deletedObjects = deleteObjectsResult.getDeletedObjects();
                    try {
                        for (String obj : deletedObjects) {
                            String deleteObj = URLDecoder.decode(obj, "UTF-8");
                            log.info(deleteObj);
                        }
                    } catch (UnsupportedEncodingException e) {
                        log.error("Unsupported encoding");
                    }
                }

                nextMarker = objectListing.getNextMarker();
            } while (objectListing.isTruncated());
        } catch (OSSException oe) {
            log.error("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            log.error("Error Message:" + oe.getErrorMessage());
            log.error("Error Code:" + oe.getErrorCode());
            log.error("Request ID:" + oe.getRequestId());
            log.error("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            log.error("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            log.error("Error Message:" + ce.getMessage());
        }
    }


    public ListObjectsV2Result listFile(String dirPath, String startAfter, int maxSize) {
        final ListObjectsV2Request objectsV2Request = new ListObjectsV2Request(bucket);
        objectsV2Request.setPrefix(getOssKey(dirPath));
        objectsV2Request.setMaxKeys(maxSize);
        objectsV2Request.setStartAfter(startAfter);
        try {
            return ossClient.listObjectsV2(objectsV2Request);
        } catch (OSSException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copy OSS file
     * Both inputs are full paths of OSS Objects
     *
     * @param srcKey         source path file example: qa/zeus/src_key/d_key=1/test.jar
     * @param destinationKey destination path file example: qa/zeus/dest_key/d_key=2/test.jar
     */
    public CopyObjectResult copyObject(String srcKey, String destinationKey) {
        log.info("{} copied to {}", srcKey, destinationKey);
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucket, getOssKey(srcKey), bucket, getOssKey(destinationKey));
        CopyObjectResult result = ossClient.copyObject(copyObjectRequest);
        log.info("ETag: {}, LastModified: {}" + result.getETag(), result.getLastModified());
        return result;
    }

    public URL getOssUrl(String srcKey) {
        // Set signed URL expiration time to 600 seconds (10 minutes).
        Date expiration = new Date(System.currentTimeMillis() + 600 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, getOssKey(srcKey));
        request.setExpiration(expiration);
        request.setMethod(HttpMethod.GET);
        //no-store:No content will be cached.。
        request.addHeader("Cache-Control", "no-store");
        request.setProcess("ignore-referer");
        return ossClient.generatePresignedUrl(request);
    }

}
