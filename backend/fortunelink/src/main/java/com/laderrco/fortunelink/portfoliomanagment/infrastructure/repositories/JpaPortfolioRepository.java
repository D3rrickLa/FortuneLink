package com.laderrco.fortunelink.portfoliomanagment.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;

import lombok.AllArgsConstructor;

// Infrastructure-specific JPA repo (interface)
interface SpringDataJpaPortfolioEntityRepository extends JpaRepository<Portfolio, UUID> {}


// Your actual implementation of your domain's IPortfolioRepository (class)
@Repository
@AllArgsConstructor
public class JpaPortfolioRepository implements PortfolioRepository {
    private final SpringDataJpaPortfolioEntityRepository jpaRepository; // This is the Spring Data magic
    // ... mapping ...

    @Override
    public Optional<Portfolio> findById(UUID id) {
       return jpaRepository.findById(id);
    }

    @Override
    public List<Portfolio> findByUserId(UUID id) {
        return null;
    }

    @Override
    public Portfolio save(Portfolio portfolio) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(Portfolio portfolio) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<Portfolio> findAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }
}