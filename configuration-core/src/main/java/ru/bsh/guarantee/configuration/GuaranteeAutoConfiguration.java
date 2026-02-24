package ru.bsh.guarantee.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.bsh.guarantee.balancing.BalancingGroupConfiguration;

import java.util.List;

@Configuration
@ComponentScan("ru.bsh")
@RequiredArgsConstructor
public class GuaranteeAutoConfiguration {

    private final List<BalancingGroupConfiguration> configurations;
}
