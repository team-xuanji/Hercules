package team.magic.flute.hercules.twelve.labors.feign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.magic.flute.hercules.twelve.labors.feign.exceptions.ApiException;
import team.magic.flute.hercules.twelve.labors.feign.exceptions.FailResult;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Feign Error Decoder for Business Access Gateway
 *
 * <p>Custom error decoder for handling HTTP error responses in Feign clients
 * within the Hercules business access gateway architecture.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class FeignErrorDecoder implements ErrorDecoder {
    Logger logger = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = new ApiException();
        try {
            String json = Util.toString(response.body().asReader(Charset.forName("utf-8")));
            logger.warn("Original resp json: {}", json);
            exception = new ApiException(json);
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            FailResult result = objectMapper.readValue(json, new TypeReference<FailResult>() {
            });
            result.setRemoteUrl(response.request().url());
            exception = new ApiException("API:" + methodKey + " Occur err:" + response.reason(), result);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return exception;
    }
}
