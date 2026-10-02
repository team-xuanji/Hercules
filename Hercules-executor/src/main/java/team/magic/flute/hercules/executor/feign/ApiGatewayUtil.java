package team.magic.flute.hercules.executor.feign;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


@UtilityClass
public class ApiGatewayUtil {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String HEADER_SERVICE = "X-Caller-Service";
    private static final String HEADER_TIMESTAMP = "X-Caller-Timestamp";
    private static final String HEADER_SIGN = "X-Caller-Sign";
    private static final String HEADER_KEY = "X-Caller-Key";


    public static Map<String, String> generateGatewayHeader(String url, String caller, String secret) throws URISyntaxException {
        return generateGatewayHeader(new URI(url), caller, secret);
    }

    public static Map<String, String> generateGatewayHeader(URI uri, String caller, String secret) {
        Map<String, String> headers = new HashMap<>();

        String[] pathSegments = pathSegments(uri.getPath(), 3);
        String contextPath = pathSegments[0];
        String version = pathSegments.length > 1 ? pathSegments[1] : "";
        String requestPath = pathSegments.length > 2 ? "/" + pathSegments[2] : "";

        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String sign = generateSign(contextPath, contextPath, version, timestamp, secret, requestPath);

        headers.put(HEADER_SERVICE, contextPath);
        headers.put(HEADER_TIMESTAMP, timestamp);
        headers.put(HEADER_SIGN, sign);
        headers.put(HEADER_KEY, caller);

        return headers;
    }


    private static String generateSign(String callerService, String contextPath, String version, String timestamp, String serviceSecret, String requestPath) {
        String sign = "";
        if (callerService == null || callerService.equals("") || contextPath == null || contextPath.equals("")
                || timestamp == null || timestamp.equals("") || serviceSecret == null || serviceSecret.equals("")) {
            return sign;
        }
        Map<String, String> map = new LinkedHashMap<>();
        map.put("callerService", callerService);
        map.put("contextPath", contextPath);
        try {
            if (requestPath != null) {
                StringBuilder sb = new StringBuilder();
                for (String part : requestPath.split("/")) {
                    sb.append("/").append(URLEncoder.encode(part, "utf-8"));
                }
                map.put("requestPath", sb.toString().substring(1));
            }
            map.put("timestamp", timestamp);
            map.put("v", version);
            sign = generateMD5Sign(serviceSecret, map);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
        return sign;
    }

    private static String generateMD5Sign(String secret, Map<String, String> parameters) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = md5.digest(generateConcatSign(secret, parameters).getBytes("utf-8"));
        return byteToHex(bytes);
    }

    private static String generateConcatSign(String secret, Map<String, String> parameters) {
        StringBuilder sb = new StringBuilder().append(secret);
        Set<String> keys = parameters.keySet();
        for (String key : keys) {
            sb.append(key).append(parameters.get(key));
        }
        return sb.append(secret).toString();
    }

    private static String byteToHex(byte[] bytesIn) {
        StringBuilder sb = new StringBuilder();
        for (byte byteIn : bytesIn) {
            String bt = Integer.toHexString(byteIn & 0xff);
            if (bt.length() == 1) {
                sb.append(0).append(bt);
            } else {
                sb.append(bt);
            }
        }
        return sb.toString().toUpperCase();
    }


    /**
     * <pre>
     *     ("a/b/c", 1) -> ["a/b/c"]
     *     ("a/b/c", 2) -> ["a", "b/c"]
     * </pre>
     *
     * @param path
     * @param max
     * @return
     */
    public static String[] pathSegments(String path, int max) {
        if ("".equals(path) || "/".equals(path) || "//".equals(path)) {
            return new String[]{""};
        }
        int beginIndex = 0, endIndex = path.length();
        if (path.charAt(beginIndex) == '/') {
            beginIndex++;
        }
        if (path.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return StringUtils.splitByWholeSeparatorPreserveAllTokens(path.substring(beginIndex, endIndex), "/", max);
    }
}
