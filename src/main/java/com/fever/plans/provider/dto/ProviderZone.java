package com.fever.plans.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.math.BigDecimal;

/** XML representation of a price `zone` within a provider plan. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderZone {
    @JacksonXmlProperty(isAttribute = true)
    public BigDecimal price;
}
