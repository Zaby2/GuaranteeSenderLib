package ru.bsh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bsh.balancing.Balancer;
import ru.bsh.balancing.WeightedLoadBalancer;
import ru.bsh.breaker.CircuitBreaker;
import ru.bsh.breaker.CircuitBreakerManager;
import ru.bsh.converter.GuaranteeSenderDtoConverter;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.configuration.GuaranteeSenderConfiguration;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuaranteeSenderProxyImpl<T> implements GuaranteeSenderProxy<T> {

    private final GuaranteeSenderConfiguration configuration;
    private final Balancer balancer;
    private final CircuitBreakerManager circuitBreakerManager;

    private final GuaranteeSenderDtoConverter<T> converter = new GuaranteeSenderDtoConverter<>();

    public GuaranteeSenderProxyImpl(GuaranteeSenderConfiguration configuration) {
        var mainGroup = configuration.getBalancingGroupConfigurations()
                .stream()
                .filter(conf -> conf.getIsMain() == true)
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
    }

    @Override
    public void send(T request) {
        var dtoToSend = converter.convert(request);
        var isSend =  false;
        try {
            while (!isSend) {
                var selectedProvider = balancer.choose();
                if (Objects.isNull(selectedProvider)) {
                    throw new RuntimeException("Для главной группы не найдено доступного маршрута");
                }
                try {
                    circuitBreakerManager.callWithCircuitBreaker(selectedProvider,
                            sender -> sender.send(dtoToSend));
                    isSend = true;
                } catch (Exception e) {
                    log.error("Ошибка отпрвки в главной группе для provider {}", selectedProvider.getName());
                }
            }
        } catch (Exception e) {
            log.error("Отправка через главную группу невозможна");
            // call method to buffer sender
        }
    }
}
