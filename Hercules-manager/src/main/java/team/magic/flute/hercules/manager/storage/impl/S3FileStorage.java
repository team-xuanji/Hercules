package team.magic.flute.hercules.manager.storage.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import team.magic.flute.hercules.manager.exception.PluginRegisterFailedException;
import team.magic.flute.hercules.manager.storage.FileStorage;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
//@Component("S3_FILE_STORAGE")
public class S3FileStorage implements FileStorage {

    private Cache<String,URL> s3HttpUrlCache;

    @Value("${file-storage.s3.endpoint}")
    private String endPoint;
    @Value("${file-storage.s3.bucket}")
    private String bucket;
    @Value("${file-storage.s3.access}")
    private String accessKeyId;
    @Value("${file-storage.s3.secret}")
    private String accessKeySecret;
    @Value("${file-storage.s3.root-path}")
    private String rootPath;
    @Value("${file-storage.s3.support-http-download}")
    private boolean supportHttpDownload;

    private final static String S3_URL_TEMPLATE = "S3://{bucket}.{endpoint}/{path}";

    private static final String S3_SEPARATOR = "/";
    private S3Client s3Client;

    @PostConstruct
    private void initS3Client(){
        s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                accessKeyId,
                                accessKeySecret)))
                .region(Region.AWS_GLOBAL)
                .endpointOverride(URI.create(endPoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
        s3HttpUrlCache = Caffeine.newBuilder()
                .expireAfterWrite(8, TimeUnit.MINUTES)
                .initialCapacity(10)
                .maximumSize(20000)
                .build(this::getS3Url);
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
                String fileName = resource.substring(resource.lastIndexOf(S3_SEPARATOR)+1);
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
            String path = getS3Url(pluginGroup,file);
            result.add(path);
        }
        return result;
    }

    private String getS3Url(String pluginName, MultipartFile file){
        String fullPath = getFullRemotePath(pluginName,file);
        return  S3_URL_TEMPLATE.replace("{bucket}",bucket)
                .replace("{endpoint}",endPoint)
                .replace("{path}",fullPath);
    }
    private String getFullRemotePath(String pluginName, MultipartFile file){
        String path = getPluginStorageDirPath(pluginName)+file.getOriginalFilename();
        if (StringUtils.startsWith(path, S3_SEPARATOR)) {
            return StringUtils.replaceOnce(path, S3_SEPARATOR, "");
        }
        return path;
    }

    private String getPluginStorageDirPath(String pluginGroup){
        if(rootPath.endsWith(S3_SEPARATOR)){
            return rootPath+DigestUtils.md5Hex(pluginGroup.trim())+ S3_SEPARATOR;
        }else{
            return rootPath+ S3_SEPARATOR +DigestUtils.md5Hex(pluginGroup.trim())+ S3_SEPARATOR;
        }

    }


    @Override
    public void download(String pluginGroup, String fileName, HttpServletResponse response) {
        throw new UnsupportedOperationException("OSS file download is currently not supported");
    }

    @Override
    public void download2LocalFile(String ossKey, File destFile) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .key(getS3Key(ossKey))
                    .bucket(bucket)
                    .build();
            s3Client.getObject(objectRequest,destFile.toPath());
            log.info("OSS file downloaded successfully, OssFile={}, target file={}", ossKey, destFile);
        } catch (Exception e) {
            log.error("OSS file download failed, OssFile={}, target file={}", ossKey, destFile, e);
            throw e;
        }
    }

    @Override
    public InputStream getDownloadStream(String ossKey) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .key(getS3Key(ossKey))
                .bucket(bucket)
                .build();
        return s3Client.getObject(objectRequest);
    }

    @Override
    public void upload(String pluginGroup, MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if(StringUtils.isNotBlank(fileName)){
                String remotePath = getFullRemotePath(pluginGroup,file);
                upload(file,remotePath,true);
            }
        }
    }

    @Override
    public void delete(String pluginGroup) {
        String remoteRootPath = getPluginStorageDirPath(pluginGroup);
        deleteDir(remoteRootPath,true);
    }



    @Override
    public URL parse2HttpUrls(String originalUrl) {
        String key = originalUrl.replaceAll("(S3|s3)://.*?/","");
        if(s3HttpUrlCache.getIfPresent(key)==null){
            synchronized (this){
                if(s3HttpUrlCache.getIfPresent(key)==null){
                    s3HttpUrlCache.put(key, getS3Url(key));
                }
            }
        }
        return s3HttpUrlCache.getIfPresent(key);
    }

    public void upload(MultipartFile file, String remotePath, boolean overwrite) throws IOException {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        if(!overwrite){
            builder = builder.overrideConfiguration(bd -> bd.putHeader("If-None-Match", "*"));
        }
        PutObjectRequest objectRequest = builder
                .bucket(bucket)
                .key(getS3Key(remotePath))
                .build();
        RequestBody requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
        s3Client.putObject(objectRequest,requestBody);
    }

    private String getS3Key(String path) {
        if (StringUtils.startsWith(path, S3_SEPARATOR)) {
            return StringUtils.replaceOnce(path, S3_SEPARATOR, "");
        }
        return path;
    }

    public void deleteDir(String dirPath,boolean recursion) {
        if(!dirPath.endsWith(S3_SEPARATOR)){
            throw new IllegalArgumentException("When deleting OSS directory, the directory path must end with /");
        }
        try {
            String key = getS3Key(dirPath);
            if(recursion){
                ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                        .bucket(bucket).prefix(key).build();
                ListObjectsResponse objectsResponse = s3Client.listObjects(listObjectsRequest);

                while (true) {
                    ArrayList<ObjectIdentifier> objects = new ArrayList<>();

                    for (S3Object s3Object : objectsResponse.contents()) {
                        objects.add(ObjectIdentifier.builder().key(s3Object.key()).build());
                    }

                    s3Client.deleteObjects(
                            DeleteObjectsRequest.builder().bucket(bucket).delete(Delete.builder().objects(objects).build()).build()
                    );
                    if (objectsResponse.isTruncated()) {
                        objectsResponse = s3Client.listObjects(listObjectsRequest);
                        continue;
                    }
                    break;
                }
            }else{
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            }
        } catch (Exception oe) {
            log.error("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            log.error("Error Message:" + oe.getMessage());
        }
    }



    public URL getS3Url(String srcKey) {
        try(S3Presigner resigned = S3Presigner.create()){
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(getS3Key(srcKey))
                    .overrideConfiguration(b -> b
                            .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .putHeader("Pragma", "no-cache")
                            .putHeader("Expires", "0"))
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) // 10 minutes validity period
                    .getObjectRequest(getObjectRequest)
                    .build();
            return resigned.presignGetObject(presignRequest).url();
        }
    }

}
