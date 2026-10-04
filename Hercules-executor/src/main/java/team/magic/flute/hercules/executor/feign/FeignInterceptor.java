package team.magic.flute.hercules.executor.feign;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import team.magic.flute.hercules.executor.feign.bean.ApiConfigBean;
import team.magic.flute.hercules.executor.feign.exceptions.ApiException;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class FeignInterceptor implements RequestInterceptor {

    @Setter
    @Accessors(chain = true)
    private String caller;

    @Setter
    @Accessors(chain = true)
    private String secret;

    private Map<String, Collection<String>> extraHeader = new HashMap<>();

    public FeignInterceptor() {
    }

    public FeignInterceptor(String caller, String secret) {
        this.caller = caller;
        this.secret = secret;
    }

    public FeignInterceptor(ApiConfigBean config) {
        this.caller = config.getCaller();
        this.secret = config.getSign();
        Map<String, String> headers = config.getHeaders();
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            headers.entrySet()
                    .stream()
                    .filter(e -> StringUtils.isNotBlank(e.getValue()))
                    .forEach(e -> this.extraHeader.put(e.getKey(), Collections.singletonList(e.getValue())));
        }
    }

    @Override
    public void apply(RequestTemplate template) {
        try {
            String url = template.feignTarget().url();
            Map<String, Collection<String>> gateHeaders = ApiGatewayUtil.generateGatewayHeader(url + template.url(), caller, secret)
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            kv -> Collections.singletonList(kv.getValue())));

            gateHeaders.put("RequestTime", Collections.singletonList(String.valueOf(System.currentTimeMillis())));
            if (Objects.nonNull(extraHeader) && !extraHeader.isEmpty()) {
                gateHeaders.putAll(extraHeader);
            }
            gateHeaders.forEach(template::header);
        } catch (URISyntaxException e) {
            throw new ApiException(e.getMessage());
        }

    }

}
