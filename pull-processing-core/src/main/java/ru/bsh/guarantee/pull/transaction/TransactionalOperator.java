package ru.bsh.guarantee.pull.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;
import ru.bsh.guarantee.pull.configuration.db.SqlPullProcessorConfiguration;
import ru.bsh.guarantee.repository.GuaranteeJpaRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "guarantee.sql.enabled", havingValue = "true")
public class TransactionalOperator {

    private final GuaranteeJpaRepository repository;
    private final SqlPullProcessorConfiguration configuration;

    @Transactional(readOnly = true)
    public List<SqlGuaranteeEntity> getEntities() {
        return repository.findDataToSend(configuration.getReadLimit());
    }

    @Transactional
    public int deleteEntities() {
        var result = repository.findTopSentIdsOrderByCreatedAtAsc(configuration.getCleanLimit());
        repository.deleteByIdIn(result);
        return result.size();
    }
}
