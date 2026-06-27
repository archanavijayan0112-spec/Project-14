package com.floodguard.repository;

import com.floodguard.entity.Resource;
import com.floodguard.enums.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByCategory(ResourceCategory category);

    List<Resource> findByAssignedToZone(String zone);

    /**
     * Find all resources that are below their critical threshold.
     */
    @Query("SELECT r FROM Resource r WHERE r.criticalThreshold IS NOT NULL AND r.availableQuantity <= r.criticalThreshold")
    List<Resource> findCriticallyLowResources();

    /**
     * Find all resources that are below their warning threshold.
     */
    @Query("SELECT r FROM Resource r WHERE r.warningThreshold IS NOT NULL AND r.availableQuantity <= r.warningThreshold")
    List<Resource> findLowResources();

    long countByCategoryAndAssignedToZone(ResourceCategory category, String zone);
}
