package com.fever.plans.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;

/** XML representation of a provider `plan` element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderPlan {
    @JacksonXmlProperty(isAttribute = true, localName = "plan_id")
    public String id;

    @JacksonXmlProperty(isAttribute = true, localName = "plan_start_date")
    public String start;

    @JacksonXmlProperty(isAttribute = true, localName = "plan_end_date")
    public String end;

    @JacksonXmlProperty(localName = "zone")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<ProviderZone> zones = new ArrayList<>();
}
