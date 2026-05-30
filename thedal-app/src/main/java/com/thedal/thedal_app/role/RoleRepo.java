package com.thedal.thedal_app.role;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepo extends JpaRepository<Role, Long> {
    
    Optional<Role> findByRoleName(String roleName);

	@Query("SELECT r FROM Role r WHERE LOWER(r.roleName) = LOWER(:role) AND r.accountId = :accountId")
	Optional<Role> findByRoleNameAndAccountId(String role, Long accountId);
	
	List<Role> findByAccountIdOrAccountIdOrderByIdAsc(Long accountId, Long fallbackAccountId);

	//List<Role> findByRoleNameAndAccountIdOrAccountId(String upperCase, Long id, long l);
	
	@Query("SELECT r FROM Role r WHERE r.roleName = :roleName AND (r.accountId = :accountId OR r.accountId = :fallbackAccountId)")
	List<Role> findByRoleNameAndAccountIdOrAccountId(@Param("roleName") String roleName, @Param("accountId") Long accountId, @Param("fallbackAccountId") Long fallbackAccountId);

	List<Role> findByAccountId(long accountId);

	Optional<Role> findByIdAndAccountId(Long roleId, Long id);

	@Modifying
	@Query(value = "INSERT INTO role (role_name, permission, role_permission, description, account_id) " +
	               "VALUES (:roleName, :permission, CAST(:rolePermission AS jsonb), :description, :accountId)", 
	               nativeQuery = true)
	void insertRole(@Param("roleName") String roleName,
	                @Param("permission") Integer permission,
	                @Param("rolePermission") String rolePermission, 
	                @Param("description") String description,
	                @Param("accountId") Long accountId);

	@Modifying
	@Query(value = "UPDATE role SET role_name = :roleName, permission = :permission, role_permission = CAST(:rolePermission AS jsonb), description = :description, account_id = :accountId WHERE id = :roleId", 
	       nativeQuery = true)
	void updateRole(@Param("roleId") Long roleId,
	                @Param("roleName") String roleName,
	                @Param("permission") Integer permission,
	                @Param("rolePermission") String rolePermission, 
	                @Param("description") String description,
	                @Param("accountId") Long accountId);


	@Modifying
    @Query(value = "INSERT INTO role (role_name, role_permission, description, account_id) " +
                  "VALUES (:roleName, CAST(:rolePermission AS jsonb), :description, :accountId)", 
           nativeQuery = true)
    void insertRole(String roleName, String rolePermission, String description, Long accountId);
}