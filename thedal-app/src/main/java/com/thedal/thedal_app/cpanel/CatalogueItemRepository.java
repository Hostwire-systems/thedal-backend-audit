package com.thedal.thedal_app.cpanel;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogueItemRepository extends JpaRepository<CatalogueItem, Long> {

    List<CatalogueItem> findByAccountId(Long accountId);

    Optional<CatalogueItem> findByIdAndAccountId(Long id, Long accountId);

    boolean existsByNameAndAccountId(String name, Long accountId);

    @Modifying
    @Query("DELETE FROM CatalogueItem ci WHERE ci.accountId = :accountId AND ci.id IN :itemIds")
    int deleteByAccountIdAndIds(Long accountId, List<Long> itemIds);

    @Modifying
    @Query("DELETE FROM CatalogueItem ci WHERE ci.accountId = :accountId")
    int deleteByAccountId(Long accountId);

	List<CatalogueItem> findByAccountIdOrderByOrderIndexAsc(Long accountId);

	@Query("SELECT MAX(ci.orderIndex) FROM CatalogueItem ci WHERE ci.accountId = :accountId")
    Integer findMaxOrderIndexByAccountId(Long accountId);

	List<CatalogueItem> findByAccountIdAndActiveTrue(Long accountId);
}