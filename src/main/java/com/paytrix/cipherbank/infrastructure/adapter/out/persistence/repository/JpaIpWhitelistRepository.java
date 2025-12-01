package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.IpWhitelist;
import com.paytrix.cipherbank.application.port.out.IpWhitelistRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface JpaIpWhitelistRepository extends JpaRepository<IpWhitelist, Long>, IpWhitelistRepositoryPort {

    // Custom query with explicit comparison for MySQL tinyint(1) compatibility
    @Query("SELECT i FROM IpWhitelist i WHERE i.active = true")
    // Spring JPA automatically maps this to SELECT * FROM ip_whitelist WHERE active = true
    List<IpWhitelist> findAllByActiveTrue();

    @Override
    default List<IpWhitelist> findAllActive() {
        return findAllByActiveTrue();
    }
}
