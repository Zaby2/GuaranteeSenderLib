package ru.bsh.guarantee.balancing;

import lombok.Data;
import ru.bsh.guarantee.sender.GuaranteeSender;

@Data
public class BalancingProvider {

    private GuaranteeSender sender;
    private String name;
    private Long weight;
}
