package com.axisrooms.homeyhuts.bean;

import com.axisrooms.homeyhuts.util.Constants;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {
    @JsonProperty("startDate")
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate startDate;

    @JsonProperty("endDate")
    @JsonFormat(pattern = Constants.DATE_PATTERN)
    private LocalDate endDate;
    @JsonProperty("inventory")
    private Integer inventory;
    @JsonProperty("ratePlanId")
    private String ratePlanId;
    @JsonProperty("closedDates")
    private List<Period> closedDates;
}
