package team.magic.flute.hercules.twelve.labors;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.twelve.labors.util.SqlTemplateUtil;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class Test02 {
    @Test
    public void test01() throws IOException, TemplateException {
        Map<String,Object> params = new HashMap<>();
        params.put("name","asd");
        String sqlTemplate = "SELECT * FROM test01 <#if name??> where name = '${name}' </#if>";
        StringWriter out = new StringWriter();
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("dynamicSQL", sqlTemplate);
        // Set template loader
        cfg.setTemplateLoader(stringLoader);
        // Use template
        Template template = cfg.getTemplate("dynamicSQL");
        template.process(params, out);
        System.out.println(out);
    }

    @Test
    public void test02(){
        Map<String,String> params = new HashMap<>();
        params.put("name","asd");
        String sqlTemplate = "SELECT * FROM test01 <#if name??> where name = '${name}' </#if>";
        System.out.println(SqlTemplateUtil.replaceSqlParams(sqlTemplate,params));
    }
}
