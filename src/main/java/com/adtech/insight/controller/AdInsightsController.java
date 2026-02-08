package com.adtech.insight.controller;


import com.adtech.insight.configuration.TenantResolver;
import com.adtech.insight.dto.MetricResponse;
import com.adtech.insight.dto.MetricType;
import com.adtech.insight.exception.InvalidTimeRangeException;
import com.adtech.insight.service.AdInsightsService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/ad")
@RequiredArgsConstructor
public class AdInsightsController {

    private final AdInsightsService service;
    private final TenantResolver tenantResolver;;

    @GetMapping("/{campaignId}/clicks")
    public MetricResponse clicks(
            Authentication authentication,
            @PathVariable @NotBlank String campaignId,
            @RequestParam @NotNull Instant from,
            @RequestParam @NotNull Instant to) {
        String tenant = tenantResolver.resolveTenant(authentication);

        if (from.isAfter(to)) {
            throw new InvalidTimeRangeException(from, to);
        }

        return service.getMetric(
                tenant, campaignId, MetricType.CLICKS, from, to);
    }

    @GetMapping("/{campaignId}/impressions")
    public MetricResponse impressions(
            @RequestHeader("X-Tenant-Id") String tenant,
            @PathVariable String campaignId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidTimeRangeException(from, to);
        }

        return service.getMetric(
                tenant, campaignId, MetricType.IMPRESSIONS, from, to);
    }

    @GetMapping("/{campaignId}/clickToBasket")
    public MetricResponse clickToBasket(
            @RequestHeader("X-Tenant-Id") String tenant,
            @PathVariable String campaignId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidTimeRangeException(from, to);
        }

        return service.getMetric(
                tenant, campaignId, MetricType.CLICK_TO_BASKET, from, to);
    }
}
