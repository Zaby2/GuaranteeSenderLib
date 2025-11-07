package ru.bsh.guarantee.configuration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;
import ru.bsh.guarantee.balancing.CircuitBreakerConfiguration;
import service.SignatureService;

import java.util.List;

@RequiredArgsConstructor
@Data
public class GuaranteeSenderConfiguration {

    private final SignatureService signatureService;
    private final CircuitBreakerConfiguration circuitBreakerConfiguration;
    private final List<BalancingGroupConfiguration> balancingGroupConfigurations;
}
