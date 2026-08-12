package com.fever.plans.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;

/** XML representation of the provider's `planList` root element. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderResponse {
    public Output output = new Output();

    /** XML representation of the root `output` element. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output {
        @JacksonXmlProperty(localName = "base_plan")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<ProviderBasePlan> basePlans = new ArrayList<>();
    }
}
