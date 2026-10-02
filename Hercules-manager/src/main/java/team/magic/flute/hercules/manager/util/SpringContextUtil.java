package team.magic.flute.hercules.manager.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;


@UtilityClass
@Slf4j
public class SpringContextUtil {

    private static ApplicationContext applicationContext;


    public static synchronized void setApplicationContext(ApplicationContext applicationContext)throws BeansException  {
        if(SpringContextUtil.applicationContext==null){
            SpringContextUtil.applicationContext = applicationContext;
        }
    }

    public static synchronized ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String beanId) throws BeansException {
        return applicationContext.getBean(beanId);
    }

    public static String getExceptionProfileInformation(Exception e){
        try(StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            return sw.toString().substring(0,500);
        }catch (Exception exception){
            log.error(exception.getMessage(),exception);
        }
        return e!=null?e.getMessage():"";
    }
}
