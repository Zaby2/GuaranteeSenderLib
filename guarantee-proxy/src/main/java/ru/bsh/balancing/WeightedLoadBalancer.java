package ru.bsh.balancing;

import ru.bsh.breaker.CircuitBreakerManager;
import ru.bsh.guarantee.balancing.BalancingProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedLoadBalancer implements Balancer {

    private final List<BalancingProvider> providers;
    private final CircuitBreakerManager manager;

    public WeightedLoadBalancer(List<BalancingProvider> providers, CircuitBreakerManager manager) {
        this.providers = providers;
        this.manager = manager;
    }

    @Override
    public BalancingProvider choose() {
        var openStatesNames = manager.getCircuitBreakersByName().entrySet().stream()
                .filter(entry -> entry.getValue().allowRequest())
                .map(Map.Entry::getKey)
                .toList();

        var accessibleProviders = providers.stream()
                .filter(provider -> openStatesNames.contains(provider.getName()))
                .toList();
        if (accessibleProviders.isEmpty()) {
            return null;
        }

        var randomValue = ThreadLocalRandom.current()
                .nextLong(getAccessibleTotalWeight(accessibleProviders));
        var cumulativeWeight = 0;

        for (var provider : accessibleProviders) {
            cumulativeWeight += provider.getWeight();
            if (randomValue < cumulativeWeight) {
                return provider;
            }
        }
        return providers.getLast();
    }

    private long getAccessibleTotalWeight(List<BalancingProvider> accessibleProvides) {
        return accessibleProvides.stream()
                .mapToLong(BalancingProvider::getWeight)
                .sum();
    }
}
