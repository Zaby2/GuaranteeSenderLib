package ru.bsh.guarantee.balancing;


import lombok.Data;
import ru.bsh.guarantee.dto.BufferType;

import java.util.List;

@Data
public class BalancingGroupConfiguration {

    private String name;
    private List<BalancingProvider> provider;
    private BufferType type;
}
