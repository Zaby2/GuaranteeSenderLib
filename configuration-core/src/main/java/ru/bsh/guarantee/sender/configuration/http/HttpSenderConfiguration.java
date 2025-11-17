package ru.bsh.guarantee.sender.configuration.http;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import ru.bsh.guarantee.sender.configuration.retry.RetryConfiguration;

@Data
public class HttpSenderConfiguration {

    private String url;
    private HttpHeaders headers;
    private RetryConfiguration retryConfiguration;
}
