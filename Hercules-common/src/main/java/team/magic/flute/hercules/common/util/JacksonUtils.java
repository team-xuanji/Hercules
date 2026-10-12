package team.magic.flute.hercules.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson JSON utility class for object serialization and deserialization.
 *
 * <p>This utility class provides a pre-configured ObjectMapper instance with optimized settings for
 * common JSON processing tasks. It includes support for Java 8 time types and follows best
 * practices for JSON handling.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Pre-configured ObjectMapper with sensible defaults
 *   <li>Java 8 time support (LocalDateTime, LocalDate, LocalTime)
 *   <li>Null value exclusion in serialization
 *   <li>Lenient deserialization (ignores unknown properties)
 *   <li>Consistent date/time formatting across the application
 * </ul>
 *
 * <p>Date/Time formats used:
 *
 * <ul>
 *   <li>DateTime: "yyyy-MM-dd HH:mm:ss"
 *   <li>Date: "yyyy-MM-dd"
 *   <li>Time: "HH:mm:ss"
 * </ul>
 *
 * <p>Built on Jackson 3 (tools.jackson): the mapper is created through {@link JsonMapper.Builder};
 * java.time serialization uses the {@code databind.ext.javatime} serializers that replaced the old
 * JavaTimeModule. All read/write methods throw the unchecked {@code
 * tools.jackson.core.JacksonException}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Serialize object to JSON
 * MyObject obj = new MyObject("test", LocalDateTime.now());
 * String json = JacksonUtils.writeValueAsString(obj);
 *
 * // Deserialize JSON to object
 * MyObject restored = JacksonUtils.readValue(json, MyObject.class);
 *
 * // Parse JSON tree
 * JsonNode node = JacksonUtils.readTree(json);
 * String name = node.get("name").asText();
 * }</pre>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class JacksonUtils {
    private static final String BASIC_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String BASIC_DATE_FORMAT = "yyyy-MM-dd";
    private static final String BASIC_TIME_FORMAT = "HH:mm:ss";
    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        SimpleModule javaTimeFormats = new SimpleModule("HerculesJavaTimeFormats");
        javaTimeFormats.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(BASIC_DATE_TIME_FORMAT)));
        javaTimeFormats.addSerializer(
                LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(BASIC_DATE_FORMAT)));
        javaTimeFormats.addSerializer(
                LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(BASIC_TIME_FORMAT)));
        javaTimeFormats.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(BASIC_DATE_TIME_FORMAT)));
        javaTimeFormats.addDeserializer(
                LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(BASIC_DATE_FORMAT)));
        javaTimeFormats.addDeserializer(
                LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(BASIC_TIME_FORMAT)));

        return JsonMapper.builder()
                .defaultDateFormat(new SimpleDateFormat(BASIC_DATE_TIME_FORMAT))
                .changeDefaultPropertyInclusion(
                        value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(javaTimeFormats)
                .build();
    }

    /**
     * Get the pre-configured ObjectMapper instance.
     *
     * <p>Returns the shared ObjectMapper instance that is configured with Java 8 time support, null
     * exclusion, and lenient deserialization settings.
     *
     * @return the configured ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Serialize an object to JSON string.
     *
     * <p>Converts the given object to its JSON string representation using the pre-configured
     * ObjectMapper. Null values are excluded from the output.
     *
     * @param obj the object to serialize
     * @return the JSON string representation of the object
     * @throws tools.jackson.core.JacksonException if serialization fails (unchecked)
     */
    public static String writeValueAsString(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * Deserialize JSON string to object using TypeReference.
     *
     * <p>Converts a JSON string to an object of the type specified by the TypeReference. This
     * method is useful for deserializing generic types like List&lt;MyClass&gt;.
     *
     * @param <T> the target type
     * @param var1 the JSON string to deserialize
     * @param typeReference the TypeReference specifying the target type
     * @return the deserialized object
     * @throws tools.jackson.core.JacksonException if deserialization fails (unchecked)
     * @example
     *     <pre>{@code
     * String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
     * List<Person> people = JacksonUtils.readValue(json, new TypeReference<List<Person>>(){});
     * }</pre>
     */
    public static <T> T readValue(String var1, TypeReference<T> typeReference) {
        return OBJECT_MAPPER.readValue(var1, typeReference);
    }

    /**
     * Deserialize JSON string to object using Class.
     *
     * <p>Converts a JSON string to an object of the specified class type. This is the most common
     * deserialization method for simple object types.
     *
     * @param <T> the target type
     * @param var1 the JSON string to deserialize
     * @param var2 the Class object representing the target type
     * @return the deserialized object
     * @throws tools.jackson.core.JacksonException if deserialization fails (unchecked)
     * @example
     *     <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = JacksonUtils.readValue(json, Person.class);
     * }</pre>
     */
    public static <T> T readValue(String var1, Class<T> var2) {
        return OBJECT_MAPPER.readValue(var1, var2);
    }

    /**
     * Parse JSON string into a JsonNode tree.
     *
     * <p>Converts a JSON string into a JsonNode tree structure that allows for flexible navigation
     * and manipulation of JSON data without requiring a specific target class.
     *
     * @param var1 the JSON string to parse
     * @return the JsonNode representing the parsed JSON structure
     * @throws tools.jackson.core.JacksonException if parsing fails (unchecked)
     * @example
     *     <pre>{@code
     * String json = "{\"user\":{\"name\":\"John\",\"age\":30}}";
     * JsonNode root = JacksonUtils.readTree(json);
     * String name = root.get("user").get("name").asText(); // "John"
     * int age = root.get("user").get("age").asInt(); // 30
     * }</pre>
     */
    public static JsonNode readTree(String var1) {
        return OBJECT_MAPPER.readTree(var1);
    }
}
