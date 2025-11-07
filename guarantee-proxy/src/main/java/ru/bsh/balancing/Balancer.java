package ru.bsh.balancing;

import ru.bsh.guarantee.balancing.BalancingProvider;

public interface Balancer {

    BalancingProvider choose();
}
