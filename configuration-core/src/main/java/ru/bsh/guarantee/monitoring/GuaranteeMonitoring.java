package ru.bsh.guarantee.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuaranteeMonitoring {

    private final MeterRegistry meterRegistry;

    public void success(String layer, String operation) {
        Counter.builder("app.operation.total")
                .tag("layer", layer)
                .tag("operation", operation)
                .tag("result", "success")
                .register(meterRegistry)
                .increment();
    }

    public void fail(String layer, String operation) {
        Counter.builder("app.operation.total")
                .tag("layer", layer)
                .tag("operation", operation)
                .tag("result", "fail")
                .register(meterRegistry)
                .increment();
    }
}
