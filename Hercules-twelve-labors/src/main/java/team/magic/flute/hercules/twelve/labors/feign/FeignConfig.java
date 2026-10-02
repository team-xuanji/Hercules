package team.magic.flute.hercules.twelve.labors.feign;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Feign Configuration for Business Access Gateway
 *
 * <p>Static configuration class providing pre-configured Feign components for
 * HTTP client communication within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class FeignConfig {
    static ObjectMapper objectMapper;
    static OkHttpClient okHttpClient;
    static feign.okhttp.OkHttpClient feignOkHttpClient;
    static RequestInterceptor feignInterceptor;
    static ErrorDecoder feignErrorDecoder;
    static JacksonEncoder jacksonEncoder;
    static JacksonDecoder jacksonDecoder;
    static Slf4jLogger slf4jLogger;

    static {
        SimpleModule module = new SimpleModule()
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper = new ObjectMapper()
                .registerModule(module)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        okHttpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .build();
        feignOkHttpClient = new feign.okhttp.OkHttpClient(okHttpClient);

        feignInterceptor = new FeignInterceptor();

        feignErrorDecoder = new FeignErrorDecoder();

        jacksonEncoder = new JacksonEncoder(objectMapper);

        jacksonDecoder = new JacksonDecoder(objectMapper);

        slf4jLogger = new Slf4jLogger();
    }

    public static RequestInterceptor feignInterceptor() {
        return feignInterceptor;
    }

    public static ErrorDecoder feignErrorDecoder() {
        return feignErrorDecoder;
    }

    public static feign.okhttp.OkHttpClient okHttpClient() {
        return feignOkHttpClient;
    }

    public static Slf4jLogger slf4jLogger() {
        return slf4jLogger;
    }

    public static JacksonEncoder jacksonEncoder() {
        return jacksonEncoder;
    }

    public static JacksonDecoder jacksonDecoder() {
        return jacksonDecoder;
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }
}
