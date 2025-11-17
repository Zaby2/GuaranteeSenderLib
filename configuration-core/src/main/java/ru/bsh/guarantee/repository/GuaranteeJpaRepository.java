package ru.bsh.guarantee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

public interface GuaranteeJpaRepository extends JpaRepository<SqlGuaranteeEntity, Long> {
}
