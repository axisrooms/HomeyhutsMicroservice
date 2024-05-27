package com.axisrooms.homeyhuts.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RatePlanDescription {
    @JsonProperty("roomId")
    private String                      roomId;
    @JsonProperty("currency")
    private String                      currency;
    @JsonProperty("occupancies")
    private List<String>             occupancies;
    @JsonProperty("configurations")
    private List<RatePlanConfiguration> configurations;
}
