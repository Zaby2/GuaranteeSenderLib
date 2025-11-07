package ru.bsh.breaker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.sender.GuaranteeSender;

import java.util.Map;

@RequiredArgsConstructor
@Getter
public class CircuitBreakerManager {

    private final Map<String, CircuitBreaker> circuitBreakersByName;

    public void callWithCircuitBreaker(BalancingProvider provider, SenderCallable callable) {
        var cb =  circuitBreakersByName.get(provider.getName());

        if (!cb.allowRequest()) {
            throw new RuntimeException("Circuit breaker for provider " + provider.getName() + " is OPEN");
        }

        try {
            callable.call(provider.getSender());
            cb.onSuccess();
        } catch (Exception e) {
            cb.onFailure();
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface SenderCallable {
        void call(GuaranteeSender sender) throws Exception;
    }
}
