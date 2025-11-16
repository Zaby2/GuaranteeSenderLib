package ru.bsh.breaker;

import ru.bsh.guarantee.balancing.BalancingProvider;
import ru.bsh.guarantee.exception.InternalGuaranteeException;
import ru.bsh.guarantee.sender.GuaranteeSender;

import java.util.Map;

public record CircuitBreakerManager(Map<String, CircuitBreaker> circuitBreakersByName) {

    public void callWithCircuitBreaker(BalancingProvider provider, SenderCallable callable) {
        var cb = circuitBreakersByName.get(provider.getName());

        if (!cb.allowRequest()) {
            throw new InternalGuaranteeException("Circuit breaker for provider "
                    + provider.getName() + " is OPEN");
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
