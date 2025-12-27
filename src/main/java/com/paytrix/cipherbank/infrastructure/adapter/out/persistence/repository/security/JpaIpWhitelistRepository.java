package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for IP whitelist entities
 * Note: This no longer implements IpWhitelistRepositoryPort directly
 * The adapter pattern is used instead (see IpWhitelistRepositoryAdapter)
 */
@Repository
public interface JpaIpWhitelistRepository extends JpaRepository<IpWhitelist, Long> {

    /**
     * Find all active IP addresses
     * Uses explicit query for MySQL tinyint(1) compatibility
     */
    @Query("SELECT i FROM IpWhitelist i WHERE i.active = true")
    List<IpWhitelist> findAllByActiveTrue();
}