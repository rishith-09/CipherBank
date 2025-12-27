package com.paytrix.cipherbank.application.port.out.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.IpWhitelist;
import java.util.List;

public interface IpWhitelistRepositoryPort {
    List<IpWhitelist> findAllActive();
}
