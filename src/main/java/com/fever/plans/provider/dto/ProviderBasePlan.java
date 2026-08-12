package com.fever.plans.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;

/** XML representation of a provider `base_plan` element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderBasePlan {
    @JacksonXmlProperty(isAttribute = true, localName = "base_plan_id")
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "sell_mode")
    public String sellMode;

    @JacksonXmlProperty(isAttribute = true)
    public String title;

    @JacksonXmlProperty(localName = "plan")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<ProviderPlan> plans = new ArrayList<>();
}
