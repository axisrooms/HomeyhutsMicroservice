package com.axisrooms.homeyhuts.controller;

import com.axisrooms.homeyhuts.manager.OTAManager;
import com.axisrooms.homeyhuts.request.InventoryRequest;
import com.axisrooms.homeyhuts.request.PriceRequest;
import com.axisrooms.homeyhuts.request.RestrictionRequest;
import com.axisrooms.homeyhuts.response.*;
import com.axisrooms.homeyhuts.util.MarshalUnmarshalUtils;
import com.axisrooms.homeyhuts.util.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Add all the api controllers, url mappings here
 */
@RestController
@RequestMapping(value = "/v1/homeyhuts")
@Api(description = "Api to communicate with HOMEYHUTS-ota Microservice from Channel Manager")
@Slf4j
public class Homeyhuts {

    //to ensure security.. exposing as it's for internal purpose only
    @Value("${microservice.communication.token}")
    private String acceptedToken;

    @Autowired
    private OTAManager otaManager;


    @GetMapping(
            path = "/{token}/getRooms/{hotelId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Fetch rooms from HOMEYHUTS ota given a hotelId.",
            response = RoomResponse.class
    )
    public ResponseEntity<?> getRooms(@PathVariable("token") String token, @PathVariable("hotelId") String hotelId) {
        ResponseEntity<?> responseEntity;
        try {
            Utils.isValid(token, hotelId, acceptedToken);
            RoomResponse roomResponse = otaManager.getRoomList(hotelId);
            responseEntity = new ResponseEntity<>(roomResponse, HttpStatus.OK);
        } catch (Throwable throwable) {
            log.error("Encountered exception while getting rooms", throwable);
            RoomResponse roomResponse = new RoomResponse();
            roomResponse.setMessage(throwable.getMessage());
            roomResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            responseEntity = new ResponseEntity<>(roomResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return responseEntity;
    }

    @GetMapping(
            path = "/{token}/getRatePlans/{hotelId}/{roomId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Fetch Rateplans (Price Configurations) from Homeyhuts ota given a hotelId and roomId.",
            response = RatePlanResponse.class
    )
    public ResponseEntity<?> getRatePlans(@PathVariable("token") String token, @PathVariable("hotelId") String hotelId,
                                          @PathVariable("roomId") String roomId) {
        ResponseEntity<?> responseEntity;
        try {
            Utils.isValid(token, hotelId, acceptedToken);
            Preconditions.checkArgument(!StringUtils.isEmpty(roomId), "RoomId cannot be null or empty");
            RatePlanResponse ratePlanResponse = otaManager.getRatePlans(hotelId, roomId);
            responseEntity = new ResponseEntity<>(ratePlanResponse, HttpStatus.OK);
        } catch (Throwable throwable) {
            log.error("Encountered exception while getting ratePlans", throwable);
            RatePlanResponse ratePlanResponse = new RatePlanResponse();
            ratePlanResponse.setMessage(throwable.getMessage());
            ratePlanResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            responseEntity = new ResponseEntity<>(ratePlanResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return responseEntity;
    }

    @PostMapping(
            path = "/updateInventory",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Updates inventory to HOMEYHUTS ota",
            response = InventoryResponse.class
    )
    public ResponseEntity<?> updateInventory(@Valid @RequestBody InventoryRequest inventoryRequest) {
        ResponseEntity<?> responseEntity;
        try {
            InventoryResponse inventoryResponse = otaManager.updateInventory(inventoryRequest);
            responseEntity = new ResponseEntity<>(inventoryResponse, HttpStatus.OK);
        } catch (Throwable throwable) {
            log.error("Encountered exception while update inventory", throwable);
            InventoryResponse inventoryResponse = new InventoryResponse();
            inventoryResponse.setMessage(throwable.getMessage());
            inventoryResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            responseEntity = new ResponseEntity<>(inventoryResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return responseEntity;
    }

    @PostMapping(
            path = "/updatePrice",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Update rates to HOMEYHUTS ota",
            response = PriceResponse.class
    )
    public ResponseEntity<?> updatePrice(@Valid @RequestBody PriceRequest priceRequest) {
        ResponseEntity<?> responseEntity;
        log.info("REQUEST"+priceRequest);
        try {
            PriceResponse priceResponse = otaManager.updatePrice(priceRequest);
            responseEntity = new ResponseEntity<>(priceResponse, HttpStatus.OK);
        } catch (Throwable throwable) {
            log.error("Encountered exception while update prices", throwable);
            PriceResponse priceResponse = new PriceResponse();
            priceResponse.setMessage(throwable.getMessage());
            priceResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            responseEntity = new ResponseEntity<>(priceResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }
        return responseEntity;
    }

    @PostMapping(
            path = "/updateRestriction",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(
            value = "Updates Restriction means open/close the rateplan and we can set closedOnArrival & closedOnDeparture options to HOMEYHUTS",
            response = RestrictionResponse.class
    )
    public ResponseEntity<?> updateRestriction(@Valid @RequestBody RestrictionRequest restrictionRequest) throws JsonProcessingException {
        ResponseEntity<?> responseEntity;
        String req = MarshalUnmarshalUtils.serialize(restrictionRequest);
        try {
            RestrictionResponse restrictionResponse = otaManager.updateRestriction(restrictionRequest);
            responseEntity = new ResponseEntity<>(restrictionResponse, HttpStatus.OK);
        } catch (Throwable throwable) {
            log.error("Encountered exception while update restrictions", throwable);
            RestrictionResponse restrictionResponse = new RestrictionResponse();
            restrictionResponse.setMessage(throwable.getMessage());
            restrictionResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            responseEntity = new ResponseEntity<>(restrictionResponse, HttpStatus.SERVICE_UNAVAILABLE);
        }
        log.info("responseEntity-----------"+responseEntity);
        return responseEntity;
    }

}
