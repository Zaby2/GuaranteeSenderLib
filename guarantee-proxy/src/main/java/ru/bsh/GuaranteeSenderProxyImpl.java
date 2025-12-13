package ru.bsh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bsh.balancing.Balancer;
import ru.bsh.balancing.WeightedLoadBalancer;
import ru.bsh.breaker.CircuitBreaker;
import ru.bsh.breaker.CircuitBreakerManager;
import ru.bsh.converter.GuaranteeSenderDtoConverter;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.configuration.GuaranteeSenderConfiguration;
import ru.bsh.guarantee.dto.BufferType;
import ru.bsh.guarantee.dto.GuaranteeSenderDto;
import ru.bsh.guarantee.exception.InternalGuaranteeException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuaranteeSenderProxyImpl<T> implements GuaranteeSenderProxy<T> {

    private final GuaranteeSenderConfiguration configuration;

    private final List<BalancingGroupConfiguration> bufferConfigs;
    private final Balancer balancer;
    private final CircuitBreakerManager circuitBreakerManager;

    private final GuaranteeSenderDtoConverter<T> converter = new GuaranteeSenderDtoConverter<>();

    public GuaranteeSenderProxyImpl(GuaranteeSenderConfiguration configuration) {
        var mainGroup = configuration.getBalancingGroupConfigurations()
                .stream()
                .filter(conf -> conf.getType() == BufferType.HTTP)
                .findFirst()
                .get()
                .getProvider();

        var circuitBreakerConfiguration = configuration.getCircuitBreakerConfiguration();
        var map = mainGroup.stream()
                .collect(Collectors.toMap(
                        BalancingProvider::getName,
                        provider -> new CircuitBreaker(
                                circuitBreakerConfiguration.getFailureThreshold(),
                                circuitBreakerConfiguration.getTimeoutMs()
                        )
                ));
        this.configuration = configuration;
        this.circuitBreakerManager = new CircuitBreakerManager(map);
        this.balancer = new WeightedLoadBalancer(mainGroup, circuitBreakerManager);
        this.bufferConfigs = configuration.getBalancingGroupConfigurations().stream()
                .filter(conf -> conf.getType() != BufferType.HTTP)
                .toList();
    }

    @Override
    public void send(T request) {
        var dtoToSend = converter.convert(request);
        send(dtoToSend);
    }

    public void send(GuaranteeSenderDto dtoToSend) {
        var selectedProvider = balancer.choose();
        var isSend = false;
        while (Objects.nonNull(selectedProvider)) {
            try {
                circuitBreakerManager.callWithCircuitBreaker(selectedProvider,
                        sender -> sender.send(dtoToSend));
                log.info("Запрос успешно отправлен через главную группу транспорта {}", selectedProvider);
                isSend = true;
                break;
            } catch (Exception e) {
                log.error("Ошибка отпрвки в главной группе для provider {}", selectedProvider.getName());
                selectedProvider = balancer.choose();
            }
        }
        if (!isSend) {
            sendToBuffer(dtoToSend);
        }
    }

    private void sendToBuffer(GuaranteeSenderDto dataToSend) {
        var objectMapper = new ObjectMapper();
        String stringData;
        try {
            stringData = objectMapper.writeValueAsString(dataToSend);
        } catch (JsonProcessingException e) {
            throw new InternalGuaranteeException(
                    String.format("Ошибка преобразования в строку при подписании %s", e.getMessage())
            );
        }
        var singnature = configuration.getSignatureService().sign(stringData);
        dataToSend.setSignature(singnature);
        for (var bufferConfig : bufferConfigs) {
            var providers = bufferConfig.getProvider().stream()
                    .sorted(Comparator.comparing(BalancingProvider::getWeight))
                    .toList();
            for (var provider : providers) {
                try {
                    provider.getSender().send(dataToSend);
                    log.info("Событие успешно отправлено через {} группы {}",
                            provider.getName(), bufferConfig.getName());
                    break;
                } catch (InternalGuaranteeException e) {
                    log.error("Ошибка отправки через {} для группы {}",
                            provider.getName(), bufferConfig.getName());
                }
            }
        } // todo продумать что будет в случае недоступности всех групп
    }
}
