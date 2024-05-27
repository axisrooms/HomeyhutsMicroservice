
package com.axisrooms.homeyhuts.generated.RatePlanInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RatePlanInfoResponse {
    @JsonProperty("data")
    private List<Datum> Data;
    @JsonProperty("message")
    private String Message;
    @JsonProperty("status")
    private String Status;
}
