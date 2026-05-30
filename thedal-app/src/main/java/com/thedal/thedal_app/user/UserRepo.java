package com.thedal.thedal_app.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.role.Role;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Long> {

    // Eagerly fetch accountEntity and role to avoid lazy loading issues in JWT filter
    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.accountEntity LEFT JOIN FETCH u.role WHERE u.id = :id")
    Optional<UserEntity> findByIdWithAccountAndRole(@Param("id") Long id);
   
    // @Query("SELECT new com.thedal.thedal_app.user.UserListDto(u.firstName, u.lastName, u.mobileNumber, u.password, p.subscription, u.isActive, u.loginExpiryDate) " +
    //        "FROM UserEntity u JOIN u.role r WHERE r.roleName = 'SUPER_ADMIN'")
    // List<UserListDto> findAllSuperAppAdminUsers();


    // @Query("SELECT new com.thedal.thedal_app.user.UserListDto(u.firstName, u.lastName, u.mobileNumber, u.password, p.subscription, u.isActive) " +
    //    "FROM UserEntity u JOIN u.role r JOIN u.profile p " +
    //    "WHERE r.roleName = 'SUPER_ADMIN'")
    // List<UserListDto> findUsersForSuperAdmin();

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobile);

    UserEntity findByEmailAndIsActive(String email, boolean isActive);

    UserEntity findByMobileNumberAndIsActive(String mobile, boolean isActive);

   // @Query("SELECT u FROM UserEntity u WHERE (u.email = :email OR u.mobileNumber = :mobile) AND u.isActive = :isActive ")
   @Query("SELECT u FROM UserEntity u WHERE u.email = :email OR u.mobileNumber = :mobile")
    UserEntity findByEmailOrMobileNumber(String email, String mobile);

    Optional<UserEntity> findByIdAndIsActive(Long userId, boolean isActive);

    @Query("SELECT u FROM UserEntity u WHERE (u.email LIKE %:query% OR u.mobileNumber LIKE %:query%) AND u.isActive = true ")
    List<UserEntity> findTop10EmailOrMobileNumberAndIsActive(@Param("query") String query);

	UserEntity findByEmail(String username);
    // UserEntity findByMobileNumber(String mobile);

	Optional<UserEntity> findByAccountEntity(AccountEntity account);

	Optional<UserEntity> findByAccountId(Long id);

	Optional<UserEntity> findByAccountEntityId(Long id);
	
	Page<UserEntity> findByAccountEntityId(Long accountId, Pageable pageable);
	
	long countByAccountEntityId(Long accountId);

	Optional<UserEntity> findByMobileNumber(String mobileNumber);

	Optional<UserEntity> findByIdAndAccountEntityId(Long userId, Long accountId);

	Page<UserEntity> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT u.email FROM UserEntity u")
     List<String> findAllEmails();
     
     @Query("SELECT u.mobileNumber FROM UserEntity u")
     List<String> findAllMobileNumbers();
     
     Page<UserEntity> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

     Page<UserEntity> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);

     Page<UserEntity> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
             String firstName, String lastName, Pageable pageable);

     Page<UserEntity> findByIsActiveAndFirstNameContainingIgnoreCase(
             Boolean isActive, String firstName, Pageable pageable);

     Page<UserEntity> findByIsActiveAndLastNameContainingIgnoreCase(
             Boolean isActive, String lastName, Pageable pageable);

     Page<UserEntity> findByIsActiveAndFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
             Boolean isActive, String firstName, String lastName, Pageable pageable);
    
  // Find by active status and name (search in both firstName and lastName)
     @Query("SELECT u FROM UserEntity u WHERE u.isActive = :isActive " +
            "AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
     Page<UserEntity> findByIsActiveAndNameContainingIgnoreCase(
             @Param("isActive") Boolean isActive, 
             @Param("name") String name, 
             Pageable pageable
     );

     // Find by name (search in both firstName and lastName)
     @Query("SELECT u FROM UserEntity u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
     Page<UserEntity> findByNameContainingIgnoreCase(
             @Param("name") String name, 
             Pageable pageable
     );

     // Find by active status and name parts (firstName and lastName or vice versa)
     @Query("SELECT u FROM UserEntity u WHERE u.isActive = :isActive " +
            "AND (" +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstPart, '%')) " +
            "AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :secondPart, '%'))) " +
            "OR " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :secondPart, '%')) " +
            "AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :firstPart, '%')))" +
            ")")
     Page<UserEntity> findByIsActiveAndNamePartsContainingIgnoreCase(
             @Param("isActive") Boolean isActive, 
             @Param("firstPart") String firstPart, 
             @Param("secondPart") String secondPart, 
             Pageable pageable
     );

     // Find by name parts (firstName and lastName or vice versa)
     @Query("SELECT u FROM UserEntity u WHERE " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstPart, '%')) " +
            "AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :secondPart, '%'))) " +
            "OR " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :secondPart, '%')) " +
            "AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :firstPart, '%')))")
     Page<UserEntity> findByNamePartsContainingIgnoreCase(
             @Param("firstPart") String firstPart, 
             @Param("secondPart") String secondPart, 
             Pageable pageable
     );

	List<UserEntity> findByAccountIdOrderById(Long accountId);

	Optional<UserEntity> findFirstByAccountEntityId(Long accountEntityId);
      
//	@Query("SELECT u FROM UserEntity u WHERE u.accountId = :accountId AND u.role.id = :roleId ORDER BY u.id ASC")
//    Optional<UserEntity> findFirstByAccountIdAndRoleIdOrderByIdAsc(
//        @Param("accountId") Long accountId,
//        @Param("roleId") Long roleId);

	Optional<UserEntity> findFirstByAccountEntityIdAndRoleIdOrderByIdAsc(Long accountId, long l);

	Page<UserEntity> findByIsActiveAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndMobileNumberContaining(
			Boolean isActive, String name, String name2, String mobileNumber, Pageable pageable);

	Page<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndMobileNumberContaining(
			String name, String name2, String mobileNumber, Pageable pageable);

	Page<UserEntity> findByIsActiveAndMobileNumberContaining(Boolean isActive, String mobileNumber, Pageable pageable);

	Page<UserEntity> findByMobileNumberContaining(String mobileNumber, Pageable pageable);

	Page<UserEntity> findByIsActiveAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(Boolean isActive,
			String name, String name2, Pageable pageable);

	Page<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String name, String name2,
			Pageable pageable);

	@Query("SELECT u FROM UserEntity u WHERE u.createdBy = :createdBy OR u.id = :userId")
	Page<UserEntity> findByCreatedByOrId(@Param("createdBy") String createdBy, 
	                                   @Param("userId") Long userId, 
	                                   Pageable pageable);

        // --- Cascade deactivation support (Option 1) ---
        // Direct children whose createdBy matches (email of parent)
        List<UserEntity> findByCreatedBy(String createdBy);

        // Bulk inactivate a set of users (skips already inactive)
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE UserEntity u SET u.isActive = false, u.updatedAt = :now, u.updatedBy = :updatedBy WHERE u.id IN :ids AND (u.isActive = true OR u.isActive IS NULL)")
        int bulkDeactivateByIds(@Param("ids") List<Long> ids, @Param("now") java.time.LocalDateTime now, @Param("updatedBy") String updatedBy);

	// Count total users
	long count();
	
	// Count active users
	long countByIsActive(Boolean isActive);
	
	// Count users by role name
	long countByRoleRoleName(String roleName);

	
}
