package ru.bsh.guarantee.pull.puller.impl.db;

import lombok.RequiredArgsConstructor;
import ru.bsh.guarantee.pull.configuration.db.DbPullProcessorConfiguration;
import ru.bsh.guarantee.pull.puller.PullProcessor;

// это шедулед таска и она точно будет компонентом и создаваться будет по условию наличия конфигурации в контексте

@RequiredArgsConstructor
public class PosgtgreSqlPullProcessor implements PullProcessor { // conditional on property config

    private final DbPullProcessorConfiguration configuration;

    @Override
    public void pull() {
        
    }
}
