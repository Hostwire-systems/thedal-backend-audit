package com.thedal.thedal_app.account;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

//	AccountEntity findByUserEntity(UserEntity user);
//
//	Optional<AccountEntity> findByUserEntity_Id(Long userId);

    /**
     * Get all account IDs for migration purposes
     * @return List of all account IDs
     */
    @Query("SELECT a.id FROM AccountEntity a")
    List<Long> findAllAccountIds();
    
    /**
     * Get all active account IDs (if onBoardStatus is used to filter)
     * @return List of active account IDs
     */
    @Query("SELECT a.id FROM AccountEntity a WHERE a.onBoardStatus = 1")
    List<Long> findAllActiveAccountIds();

}
