package com.adtech.insight.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantResolver {
    private final JwtTenantProperties props;
    public String resolveTenant(Authentication authentication) {

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authentication is not JWT");
        }

        String tenant = jwt.getClaimAsString(props.getTenantClaim());

        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("Tenant claim missing in JWT");
        }

        return tenant;
    }
}
