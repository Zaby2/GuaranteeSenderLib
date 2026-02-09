package ru.bsh.guarantee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bsh.guarantee.entity.SqlGuaranteeEntity;

import java.util.List;

public interface GuaranteeJpaRepository extends JpaRepository<SqlGuaranteeEntity, Long> {

    @Query(value = """
            select *
            from guarantee_data g
            where g.isSent = false
            order by g.createdAt ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SqlGuaranteeEntity> findDataToSend(@Param("limit") int limit);

    @Query(value = "SELECT sge.id FROM guarantee_data sge WHERE sge.isSent = true ORDER BY sge.createdAt ASC LIMIT :limit", nativeQuery = true)
    List<Long> findTopSentIdsOrderByCreatedAtAsc(@Param("limit") int limit);

    void deleteByIdIn(List<Long> ids);
}
