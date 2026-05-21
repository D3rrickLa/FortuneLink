package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserPreferencesEntity;


@Repository
public interface JpaUserPreferencesRepository extends JpaRepository<UserPreferencesEntity, UUID> {
}