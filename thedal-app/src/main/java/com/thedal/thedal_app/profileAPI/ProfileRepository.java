package com.thedal.thedal_app.profileAPI;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.profileAPI.dtos.BasicProfileRequest;

@Repository
public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

	Optional<ProfileEntity> findByAccountId(Long accountId);
	Optional<ProfileEntity> findByEmailAndAccountIdNot(String email, Long accountId);
    Optional<ProfileEntity> findByMobileNumberAndAccountIdNot(String mobileNumber, Long accountId);

    Optional<ProfileEntity> findByEmail(String email);
    Optional<ProfileEntity> findByMobileNumber(String mobileNumber);
	Optional<ProfileEntity> findByIdAndAccountId(BasicProfileRequest request, Long currentAccountId);
	
}