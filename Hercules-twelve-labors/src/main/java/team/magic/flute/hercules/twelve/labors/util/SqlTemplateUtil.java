package team.magic.flute.hercules.twelve.labors.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Template Parameter Replacement Utility for Business Access Gateway
 *
 * <p>Utility class for processing SQL templates with parameter substitution within
 * the Hercules business access gateway. Supports dynamic SQL generation for data exports.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class SqlTemplateUtil {
    
    /**
     * SQL template parameter pattern: ${#param_name}
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{#([^}]+)\\}");

    /**
     * Replace parameters in SQL template
     *
     * @param sqlTemplate SQL template, parameter format is ${#param_name}
     * @param params parameter mapping
     * @return SQL after replacement
     */
    @SneakyThrows({IOException.class,TemplateException.class})
    public static String replaceSqlParams(String sqlTemplate, Map<String, String> params){
        if (!StringUtils.hasText(sqlTemplate) || params == null || params.isEmpty()) {
            return sqlTemplate;
        }
        StringWriter out = new StringWriter();
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        Template template = new Template("dynamicSQL",sqlTemplate,cfg);
        template.process(params, out);
        return out.toString();
    }

    

    /**
     * Check if SQL template contains unreplaced parameters
     *
     * @param sql SQL statement
     * @return whether contains unreplaced parameters
     */
    public static boolean hasUnreplacedParams(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        return matcher.find();
    }
    
    /**
     * Get all parameter names in SQL template
     *
     * @param sqlTemplate SQL template
     * @return parameter name list
     */
    public static java.util.List<String> extractParamNames(String sqlTemplate) {
        java.util.List<String> paramNames = new java.util.ArrayList<>();
        
        if (!StringUtils.hasText(sqlTemplate)) {
            return paramNames;
        }
        
        Matcher matcher = PARAM_PATTERN.matcher(sqlTemplate);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!paramNames.contains(paramName)) {
                paramNames.add(paramName);
            }
        }
        
        return paramNames;
    }
}
