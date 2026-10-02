package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.f4b6a3.uuid.alt.GUID;
import team.magic.flute.hercules.common.classloader.LocalJarURLStreamHandlerFactory;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.EnumResponseType;
import team.magic.flute.hercules.common.plugin.PluginRegister;
import team.magic.flute.hercules.common.plugin.PluginRegisterInfo;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.util.StrFormat;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginImplInfoPo;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import team.magic.flute.hercules.manager.exception.PluginRegisterFailedException;
import team.magic.flute.hercules.manager.service.HerculesPluginImplService;
import team.magic.flute.hercules.manager.service.HerculesPluginManagerService;
import team.magic.flute.hercules.manager.service.HerculesPluginService;
import team.magic.flute.hercules.manager.storage.FileStorage;
import team.magic.flute.hercules.manager.vo.PluginRegisterImplVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HerculesPluginManagerServiceImpl implements HerculesPluginManagerService {

    @Resource(name = "OSS_FILE_STORAGE")
    private FileStorage fileStorage;
    @Autowired
    private HerculesPluginService pluginService;
    @Autowired
    private HerculesPluginImplService pluginImplService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<PluginResourceInfo> registerPlugin(MultipartFile[] pluginFiles, String pluginGroup) throws PluginRegisterFailedException {
        try{
            log.info("Starting plugin registration, registering plugin group [{}]",pluginGroup);
            if(pluginFiles==null || pluginFiles.length!=1){
                return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID,"Currently plugin upload only allows uploading one fatJar package, cannot skip upload or upload multiple files.");
            }
            // For convenience, multiple jars are not supported temporarily.
            // Since plugin implementations in the plugin package may decrease, we need to clear the information in the database first, then rewrite it to ensure information consistency.
            log.info("Removing old plugin implementations, registering plugin group [{}]",pluginGroup);
            pluginImplService.remove(new LambdaQueryWrapper<HerculesPluginImplInfoPo>()
                    .eq(HerculesPluginImplInfoPo::getPluginGroup,pluginGroup));
            // Delete files once, if we keep appending, files will accumulate and we need to save space
            log.info("Cleaning plugin group JAR packages, starting to register plugin group [{}]",pluginGroup);
            fileStorage.delete(pluginGroup);
            /*
            * In extreme cases, there may be inconsistency between remote storage and local data information
            * Because the current situation requires uploading first, then using the uploaded OSS remote information to update the database
            * Actually, there are several more suitable approaches:
            * 1. May need to abandon the database, complete all plugin information and plugin management based on OSS file management
            * 2. Find a way to extract information before uploading to OSS (such as implementing a stream-based class loader), store in database first then upload
            * 3. This approach is quite risky, load stream to write local directory files, then load information, finally delete.
            * If not careful, it might blow up the container. So try not to use it. But some people are using this method.
            * But here, I don't want to change it
            * */
            fileStorage.upload(pluginGroup,pluginFiles);
            Set<String> urls = fileStorage.getUrls(pluginGroup,pluginFiles);
            List<URL> httpUrls = urls.stream()
                    .map(x-> fileStorage.parse2HttpUrls(x))
                    .collect(Collectors.toList());
            List<HerculesPluginImplInfoPo> pluginImplList = loadPluginImplInfo(pluginGroup,httpUrls);
            if(pluginImplList.isEmpty()){
                log.warn("No plugin information detected in plugin group [{}], please check if the plugin package or OSS permissions are correct.",pluginGroup);
                processNoPluginImplResource(pluginGroup, urls);
            }else{
                processHasPluginImplResource(pluginGroup, urls, pluginImplList);
            }
            BaseResponse<PluginResourceInfo> baseResponse = BaseResponse.success(searchPlugin(null,pluginGroup));
            if(pluginImplList.isEmpty()){
                String msg = StrFormat.format("The plugin package uploaded for plugin group '{}' seems to have issues. " +
                        "We were unable to load any available plugin information from the plugin package, so we cleared all plugin implementation information for this plugin group! " +
                        "Please note: If there was previously plugin implementation information for this plugin group, this information has now been deleted.",pluginGroup);
                log.warn(msg);
                baseResponse.setMsg(msg);
            }
            return baseResponse;
        } catch (Exception e){
            log.error(e.getMessage(),e);
            throw new PluginRegisterFailedException(e.getMessage(),e);
        }
    }

    private void processHasPluginImplResource(String pluginGroup, Set<String> urls, List<HerculesPluginImplInfoPo> pluginImplList) {
        // Detect if someone else has pre-uploaded a different plugin package containing the same plugin implementation
        // If so, the plugins implemented by the two plugin packages conflict, reject the latter's request
        // If not, this is a normal new plugin, proceed with registration and database storage
        List<String> pluginHandleStrList = pluginImplList.stream()
                .map(HerculesPluginImplInfoPo::getPluginHandle)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        HerculesPluginImplInfoPo duplicatePluginImpl = pluginImplService.getOne(new LambdaQueryWrapper<HerculesPluginImplInfoPo>()
                .in(HerculesPluginImplInfoPo::getPluginHandle,pluginHandleStrList)
                .ne(HerculesPluginImplInfoPo::getPluginGroup, pluginGroup)
                .last("LIMIT 1"));
        if(duplicatePluginImpl==null){
            processNormalPlugin(pluginGroup, urls, pluginImplList);
        }else{
            processDuplicatePlugin(pluginGroup, duplicatePluginImpl);
        }
    }

    private void processNoPluginImplResource(String pluginGroup, Set<String> urls) {
        // The uploaded package has no plugin implementations, so perform the following operations: store plugin group information in database, delete plugin implementation information corresponding to the plugin group, consistent with facts
        HerculesPluginInfoPo plugin = new HerculesPluginInfoPo()
                .setPluginGroup(pluginGroup)
                .setResources(urls)
                .setRevision(GUID.v7().toString());
        pluginService.saveOrUpdate(plugin);
        // Since plugin implementation information was deleted earlier, no operation is needed here
    }

    private void processNormalPlugin(String pluginGroup, Set<String> urls, List<HerculesPluginImplInfoPo> pluginImplList) {
        pluginImplService.saveOrUpdateBatch(pluginImplList);
        // Plugin package information doesn't need to be deleted first because the primary key is consistent, only need to update database information
        HerculesPluginInfoPo etlPlugin = new HerculesPluginInfoPo();
        etlPlugin.setPluginGroup(pluginGroup)
                .setResources(urls)
                .setRevision(GUID.v7().toString());
        pluginService.saveOrUpdate(etlPlugin);
    }

    private void processDuplicatePlugin(String pluginGroup, HerculesPluginImplInfoPo duplicatePluginImpl) {
        String duplicatePluginGroup = duplicatePluginImpl.getPluginGroup();
        String duplicatePluginHandle = duplicatePluginImpl.getPluginHandle();
        String msg = StrFormat.format("Plugin registration duplicate! Please check plugin configuration! Duplicate plugin is {}, already registered by plugin group {}",duplicatePluginHandle,duplicatePluginGroup);
        try {
            // Since the file was uploaded first, if registration fails, the OSS file will actually remain, so this file needs to be deleted
            // This looks quite ugly now, mainly due to ClassLoader issues
            fileStorage.delete(pluginGroup);
        } catch (IOException ex) {
            throw new PluginRegisterFailedException(ex.getMessage(),ex);
        }
        throw new UnsupportedOperationException(msg);
    }

    private List<HerculesPluginImplInfoPo> loadPluginImplInfo(String pluginGroup,List<URL> httpUrls) throws IOException, URISyntaxException {
        List<HerculesPluginImplInfoPo> result = new ArrayList<>();
        // Actually, the more reasonable approach here is to implement a stream-based class loader and process it during file upload.
        // Now for convenience, we go through OSS once and use URLClassLoader
        // Let's leave it like this for now, don't want to deal with it, too lazy to change it
        String randPrefix = UUID.randomUUID().toString().replace("-","");
        File tempFile = Files.createTempFile(String.format(randPrefix+"_hercules_custom_plugin_%s_", pluginGroup), ".jar").toFile();
        URL resourceUrl = httpUrls.get(0);
        try(URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{tempFile.toURI().toURL()},Thread.currentThread().getContextClassLoader(),new LocalJarURLStreamHandlerFactory())){
            fileStorage.download2LocalFile(resourceUrl.toURI().getPath(), tempFile);
            ServiceLoader<PluginRegister> serviceLoader = ServiceLoader.load(PluginRegister.class,urlClassLoader);
            serviceLoader.forEach(x->{
                Map<String, PluginRegisterInfo> pluginInfo= x.getRegisterInfo();
                pluginInfo.forEach((pluginName,pluginDescriptor)->{
                    String pluginImplName = pluginDescriptor.getPluginName();
                    String desc = pluginDescriptor.getDesc();
                    String className = pluginDescriptor.getClassName();
                    HerculesPluginImplInfoPo pluginImpl = new HerculesPluginImplInfoPo()
                            .setPluginHandle(pluginImplName)
                            .setPluginGroup(pluginGroup)
                            .setImplClassName(className)
                            .setDesc(desc);
                    result.add(pluginImpl);
                });
            });
            return result;
        }finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseResponse<String> removePlugin(String pluginGroup) throws PluginRegisterFailedException{
        try{
            // Operate database first, then storage to ensure consistency
            pluginService.removeById(pluginGroup);
            pluginImplService.remove(new LambdaQueryWrapper<HerculesPluginImplInfoPo>()
                    .eq(HerculesPluginImplInfoPo::getPluginGroup,pluginGroup));
            fileStorage.delete(pluginGroup);
            return BaseResponse.success();
        }catch (Exception e){
            log.error(e.getMessage(),e);
            throw new PluginRegisterFailedException(e.getMessage(),e);
        }
    }

    @Override
    public PluginResourceInfo searchPlugin(String pluginHandle,String pluginGroup){
        if(StringUtils.isNotBlank(pluginHandle)){
            LambdaQueryWrapper<HerculesPluginImplInfoPo> lambdaQueryWrapper = new LambdaQueryWrapper<HerculesPluginImplInfoPo>()
                    .eq(HerculesPluginImplInfoPo::getPluginHandle,pluginHandle);
            if(StringUtils.isNotBlank(pluginGroup)){
                lambdaQueryWrapper = lambdaQueryWrapper.eq(HerculesPluginImplInfoPo::getPluginGroup,pluginGroup);
            }else{
                lambdaQueryWrapper = lambdaQueryWrapper.isNotNull(HerculesPluginImplInfoPo::getPluginGroup);
            }
            lambdaQueryWrapper = lambdaQueryWrapper.last("LIMIT 1");
            HerculesPluginImplInfoPo pluginImpl = pluginImplService.getOne(lambdaQueryWrapper);
            if(pluginImpl==null){
                return new PluginResourceInfo();
            }
            String searchedPluginGroup = pluginImpl.getPluginGroup();
            HerculesPluginInfoPo plugin = pluginService.getById(searchedPluginGroup);
            return fileStorage.getPlugin(plugin);
        }else if(StringUtils.isNotBlank(pluginGroup)){
            HerculesPluginInfoPo plugin = pluginService.getById(pluginGroup);
            return fileStorage.getPlugin(plugin);
        }
        return new PluginResourceInfo();
    }

    @Override
    public List<PluginRegisterImplVO> searchPluginImplInfo(String pluginGroup){
        List<HerculesPluginImplInfoPo> pluginImplList = pluginImplService.list(new LambdaQueryWrapper<HerculesPluginImplInfoPo>()
                .eq(HerculesPluginImplInfoPo::getPluginGroup,pluginGroup));
        return pluginImplList.stream().map(PluginRegisterImplVO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginResourceInfo> searchAllPluginInfo() {
        List<HerculesPluginInfoPo> pluginList = pluginService.list();
        List<PluginResourceInfo> result= new ArrayList<>();
        for (HerculesPluginInfoPo plugin : pluginList) {
            PluginResourceInfo infoDescriptor = fileStorage.getPlugin(plugin);
            result.add(infoDescriptor);
        }
        return result;
    }

}
