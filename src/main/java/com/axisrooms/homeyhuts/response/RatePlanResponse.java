package com.axisrooms.homeyhuts.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RatePlanResponse {
    @JsonProperty("message")
    private String message;
    @JsonProperty("httpStatusCode")
    private int httpStatusCode;
    @JsonProperty("ratePlanDescriptions")
    private List<RatePlanDescription> ratePlanDescriptions;
}
