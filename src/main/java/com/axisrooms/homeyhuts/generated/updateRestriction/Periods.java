
package com.axisrooms.homeyhuts.generated.updateRestriction;

import com.axisrooms.homeyhuts.bean.Period;
import com.axisrooms.homeyhuts.util.Constants;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Periods extends Period {
    @JsonProperty("endDate")
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate endDate;
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    @JsonProperty("startDate")
    private LocalDate startDate;
}
