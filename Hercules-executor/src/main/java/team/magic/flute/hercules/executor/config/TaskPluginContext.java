package team.magic.flute.hercules.executor.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.plugin.TaskPlugin;

import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class TaskPluginContext {
    private final Map<String,Map<String,TaskPluginContextDetail>> taskPluginContextDetailMap = new HashMap<>();
    private final Map<String,String> pluginGroupLatestVersion = new HashMap<>();
    private static final int MAX_KEEP_VERSIONS = 3;

    public Set<String> getPluginGroups() {
        return taskPluginContextDetailMap.keySet();
    }

    public String getPluginGroupLatestVersion(String pluginGroup) {
        return pluginGroupLatestVersion.get(pluginGroup);
    }

    public void addContextDetail(PluginResourceInfo pluginResourceInfo, URLClassLoader urlClassLoader, Map<String,TaskPlugin> taskPluginMap) {
        String pluginGroup = pluginResourceInfo.getPluginGroup();
        String pluginVersion = pluginResourceInfo.getVersionId();
        TaskPluginContextDetail taskPluginContextDetail = new TaskPluginContextDetail()
                .setPluginClassLoader(urlClassLoader)
                .setPluginGroup(pluginGroup)
                .setPluginVersion(pluginVersion)
                .setPlugins(taskPluginMap)
                .setPublishTime(LocalDateTime.now());

        taskPluginContextDetailMap.computeIfAbsent(pluginGroup, (key)->new HashMap<>())
                .putIfAbsent(pluginVersion, taskPluginContextDetail);
        pluginGroupLatestVersion.put(pluginGroup, pluginVersion);
    }

    public Map<String, TaskPlugin> getPluginMapByVersion(String pluginGroup,String pluginVersion) {
        return Optional.ofNullable(taskPluginContextDetailMap.getOrDefault(pluginGroup,new HashMap<>())
                        .getOrDefault(pluginVersion,new TaskPluginContextDetail())
                        .getPlugins())
                .orElse(new HashMap<>());
    }
    public Map<String, TaskPlugin> getLatestPluginMap(String pluginGroup) {
        return getPluginMapByVersion(pluginGroup,pluginGroupLatestVersion.get(pluginGroup));
    }

    public void initLatestPluginMap(String pluginGroup) {
        for (TaskPlugin value : getLatestPluginMap(pluginGroup).values()) {
            try{
                value.init();
            }catch (Exception e){
                log.warn("Plugin initialization error! Plugin name: [{}]",value.getName());
            }
        }
    }

    public void closeOldPlugins(String pluginGroup) {
        String latestVersion = pluginGroupLatestVersion.get(pluginGroup);
        if (StringUtils.isBlank(latestVersion)) {
            return;
        }
        Map<String,TaskPluginContextDetail> allVersionInfo = Optional.ofNullable(taskPluginContextDetailMap.get(pluginGroup))
                .orElse(new HashMap<>());
        Set<String> tryDeleteVersions = allVersionInfo.keySet().stream().filter(key -> !key.equals(latestVersion)).collect(Collectors.toSet());

        List<TaskPluginContextDetail> closedVersions = allVersionInfo.values().stream()
                        .filter(taskPluginContextDetail -> tryDeleteVersions.contains(taskPluginContextDetail.getPluginVersion()))
                .sorted(Comparator.comparing(TaskPluginContextDetail::getPublishTime)).collect(Collectors.toList());
        if(closedVersions.size()>MAX_KEEP_VERSIONS){
            closedVersions = closedVersions.stream().limit(closedVersions.size() -  MAX_KEEP_VERSIONS).collect(Collectors.toList());
        }
        for (TaskPluginContextDetail taskContextDetail : closedVersions) {
            String oldVersion = taskContextDetail.getPluginVersion();
            Map<String, TaskPlugin> plugins = taskContextDetail.getPlugins();
            if(plugins!=null){
                for (Map.Entry<String, TaskPlugin> entry : plugins.entrySet()) {
                    String pluginName = entry.getKey();
                    TaskPlugin taskPlugin = entry.getValue();
                    try{
                        taskPlugin.cleanUp();
                    } catch (Exception e) {
                        log.error("Error while stop old plugin [{}],version[{}]",pluginName,oldVersion,e);
                    }
                }
            }
            try{
                taskContextDetail.getPluginClassLoader().close();
                allVersionInfo.remove(oldVersion);
            } catch (Exception e) {
                log.error("Error while closing old ClassLoader for pluginGroup [{}],version[{}]",pluginGroup,oldVersion,e);
            }
        }
    }
}
