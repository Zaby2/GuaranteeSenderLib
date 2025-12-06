package ru.bsh.guarantee.sender.configuration.http;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import ru.bsh.guarantee.sender.configuration.retry.RetryConfiguration;

import java.util.Map;

@Data
public class HttpSenderConfiguration {

    private String url;
    private Map<String, String> headersMap;
    private RetryConfiguration retryConfiguration;
    private HttpHeaders headers;

    public HttpHeaders getHeaders() {
        if (headers != null) {
            return headers;
        }
        var headers = new HttpHeaders();
        if (headersMap != null) {
            headersMap.forEach(headers::add);
        } else {
            return null;
        }
        this.headers = headers;
        return headers;
    }
}
