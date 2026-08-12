package com.fever.plans.provider;

import com.fever.plans.provider.dto.ProviderPlanData;
import java.util.List;

public interface PlanProvider {
    List<ProviderPlanData> fetchPlans();
}
