package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserProfileEntity;

@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
  
}
