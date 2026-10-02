package team.magic.flute.hercules.common.plugin;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class PluginResourceInfo {
    private String pluginGroup;
    private String versionId;
    /**
     * The resource address may not support HTTP downloads.
     * File systems like OSS can implement anti-leeching measures.
     * If HTTP downloads are not supported,
     * then the executor and Manager must share the same OSS configuration.
     * Otherwise, how am I supposed to download the resource, my dear comrade?
     */
    private boolean canDownloadByHttp = true;
    /**
     * Theoretically, a plugin can consist of multiple resource packages, but for convenience,
     * it is best to have each resource as a single fat package.
     */
    //key:fileName value:url
    public Map<String, URI> resources;

    public synchronized void addResource(String fileName, URI fileLocationUri){
        if(resources==null){
            resources = new HashMap<>();
        }
        if(StringUtils.isNotBlank(fileName)){
            resources.put(fileName,fileLocationUri);
        }
    }
}
