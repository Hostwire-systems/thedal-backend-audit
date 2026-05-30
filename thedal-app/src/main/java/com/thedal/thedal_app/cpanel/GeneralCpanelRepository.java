package com.thedal.thedal_app.cpanel;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralCpanelRepository extends JpaRepository<GeneralCpanelEntity, Long> {

    Optional<GeneralCpanelEntity> findByCpanelName(String cpanelName);
}