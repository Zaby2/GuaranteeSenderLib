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
            select e
            from guarantee_data
            where e.isSent = false
            order by e.createdAt DESC
            """)
    List<SqlGuaranteeEntity> findDataToSend(Pageable pageable);

    @Modifying
    @Transactional
    @Query(
            value = """
                    DELETE FROM uarantee_data
                    WHERE isSent = true
                    ORDER BY createdAt
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    int deleteBatchByStatusTrue(@Param("limit") int limit);
}
