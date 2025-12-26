package ru.bsh.guarantee.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

import java.util.List;

public interface GuaranteeJpaRepository extends JpaRepository<SqlGuaranteeEntity, Long> {

    @Query("""
            select g
            from SqlGuaranteeEntity g
            where g.isSent = false
            order by g.createdAt DESC
            """)
    List<SqlGuaranteeEntity> findDataToSend(Pageable pageable);

    @Query("SELECT sge.id FROM SqlGuaranteeEntity sge WHERE sge.isSent = true ORDER BY sge.createdAt ASC")
    List<Long> findTopSentIdsOrderByCreatedAtAsc(Pageable pageable);

    void deleteByIdIn(List<Long> ids);
}
