package ru.bsh.guarantee.sender.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.sender.GuaranteeSender;
import ru.bsh.guarantee.sender.configuration.HttpSenderConfiguration;
import ru.bsh.guarantee.sender.configuration.retry.RetryConfiguration;

public class HttpSender implements GuaranteeSender {

    private final HttpSenderConfiguration configuration;
    private final RetryTemplate retryTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public HttpSender(HttpSenderConfiguration configuration) {
        this.configuration = configuration;
        this.retryTemplate = buildRetryTemplate(configuration.getRetryConfiguration());
    }

    @Override
    public void send(GuaranteeSenderDto dataToSend) {
        Class<?> clazz;
        try {
            clazz = Class.forName(dataToSend.getRequestType());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Object playLoad;
        try {
            playLoad = objectMapper.readValue(dataToSend.getRequestValue(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        HttpEntity requestEntity;
        if (configuration.getHeaders() == null) {
            requestEntity = new HttpEntity<>(playLoad);
        } else {
            requestEntity = new HttpEntity<>(playLoad, configuration.getHeaders());
        }
        retryTemplate.execute(context ->
                restTemplate.postForEntity(configuration.getUrl(), requestEntity, Void.class));
    }

    private RetryTemplate buildRetryTemplate(RetryConfiguration configuration) {
        var retryPolicy = new SimpleRetryPolicy(configuration.getMaxAttempts(),
                configuration.getExceptionsToRetry());

        var backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(configuration.getInitialInterval());
        backOffPolicy.setMultiplier(configuration.getIntervalMultiplier());
        backOffPolicy.setMaxInterval(configuration.getMaxInterval());

        var retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
