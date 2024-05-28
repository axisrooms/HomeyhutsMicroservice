package com.axisrooms.homeyhuts.manager;

import com.axisrooms.homeyhuts.bean.*;
import com.axisrooms.homeyhuts.generated.RatePlanInfo.RatePlanInfoRequest;
import com.axisrooms.homeyhuts.generated.RatePlanInfo.RatePlanInfoResponse;
import com.axisrooms.homeyhuts.generated.RatePlanInfo.Validity;
import com.axisrooms.homeyhuts.generated.productInfo.Datum;
import com.axisrooms.homeyhuts.generated.productInfo.ProductInfoRequest;
import com.axisrooms.homeyhuts.generated.productInfo.ProductInfoResponse;
import com.axisrooms.homeyhuts.generated.updateInventory.Auth;
import com.axisrooms.homeyhuts.generated.updateInventory.Data;
import com.axisrooms.homeyhuts.generated.updateInventory.InventoryUpdate;
import com.axisrooms.homeyhuts.generated.updateInventory.InventoryUpdateResponse;
import com.axisrooms.homeyhuts.generated.updatePrice.PriceUpdateResponse;
import com.axisrooms.homeyhuts.generated.updatePrice.Rate;
import com.axisrooms.homeyhuts.generated.updatePrice.RateUpdateResponse;
import com.axisrooms.homeyhuts.generated.updatePrice.UpdatePriceOTA;
import com.axisrooms.homeyhuts.generated.updateRestriction.*;
import com.axisrooms.homeyhuts.model.TransactionLog;
import com.axisrooms.homeyhuts.repository.AxisroomsOtaRepository;
import com.axisrooms.homeyhuts.request.InventoryRequest;
import com.axisrooms.homeyhuts.request.PriceRequest;
import com.axisrooms.homeyhuts.request.RestrictionRequest;
import com.axisrooms.homeyhuts.response.*;
import com.axisrooms.homeyhuts.util.MarshalUnmarshalUtils;
import com.axisrooms.homeyhuts.util.OccupancyNotSupportedException;
import com.axisrooms.homeyhuts.util.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

import static com.axisrooms.homeyhuts.util.Constants.SUCCESS;

/**
 * actual call to the external api in ota side will happen from here
 */
@Service
@Slf4j
@Primary
public class HomeyhutsOtaManager implements OTAManager {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${getRoomsUrl}")
    private String getRoomsUrl;

    @Value("${getUpdateInvUrl}")
    private String getUpdateInvUrl;

    @Value("${getUpdatePriceUrl}")
    private String getUpdatePriceUrl;

    @Value("${getProductInfoUrl}")
    private String getProductInfoUrl;

    @Value("${getUpdateRestrictionUrl}")
    private String getUpdateRestrictionUrl;

   /* @Value("${staymaker-ota.communication.userName}")
    private String username;

    @Value("${staymaker-ota.communication.password}")
    private String password;*/

    @Value("${rezolv-ota.communication.apitoken}")
    private String apiKey;

    @Autowired
    private AxisroomsOtaRepository repository;

    @Override
    public RoomResponse getRoomList(String hotelId) throws Exception {
        ProductInfoRequest productInfoRequest = buildProductInfoRequest(hotelId);
        ProductInfoResponse productInfoResponse = getProductInfo(productInfoRequest);
        RoomResponse roomResponse = buildRoomResponse(productInfoResponse);
        return roomResponse;
    }

    @Override
    public RatePlanResponse getRatePlans(String hotelId, String roomId) throws Exception {
        RatePlanInfoRequest ratePlanInfoRequest = buildRatePlanInfoRequest(hotelId, roomId);
//        ProductInfoRequest productInfoRequest = buildProductInfoRequest(hotelId);
        RatePlanInfoResponse ratePlanInfoResponse = getRatePlanInfo(ratePlanInfoRequest);
//        ProductInfoResponse productInfoResponse = getProductInfo(productInfoRequest);
        RatePlanResponse ratePlanResponse = buildRatePlansResponse(ratePlanInfoResponse, roomId);
        return ratePlanResponse;
    }

    @Override
    public InventoryResponse updateInventory(InventoryRequest inventoryRequest) throws Exception {
        TransactionLog transactionLog = new TransactionLog();
        Utils.addCommonData(inventoryRequest, transactionLog);
        InventoryResponse inventoryResponse = null;
        try {
            Utils.addCMRequest(inventoryRequest, transactionLog);
            List<InventoryUpdate> inventoryUpdateRequests = buildInventoryUpdateRequest(inventoryRequest);
            for (InventoryUpdate eachInventoryUpdate : inventoryUpdateRequests) {
                String jsonString = MarshalUnmarshalUtils.serialize(eachInventoryUpdate);
                log.info("update inventory request:: " + jsonString);
                Utils.addOTARequest(jsonString, transactionLog);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(jsonString, httpHeaders);
                ResponseEntity<String> responseEntity = restTemplate
                        .postForEntity(getUpdateInvUrl, entity, String.class);
                String response = responseEntity.getBody();
                log.info("update inventory response::" + response);
                Utils.addOTAResponse(response, transactionLog);
                InventoryUpdateResponse inventoryUpdateResponse = MarshalUnmarshalUtils.deserialize(response, InventoryUpdateResponse.class);
                inventoryResponse = buildInventoryResponse(inventoryUpdateResponse);
                Utils.addCMResponse(inventoryResponse, transactionLog);
            }

        } catch (Throwable throwable) {
            Utils.addOTAResponse(throwable, transactionLog);
            throw throwable;
        } finally {
            repository.save(transactionLog);
        }
        return inventoryResponse;
    }

    @Override
    public PriceResponse updatePrice(PriceRequest priceRequest) throws OccupancyNotSupportedException, Exception {
        TransactionLog transactionLog = new TransactionLog();
        Utils.addCommonData(priceRequest, transactionLog);
        RatePlanResponse ratePlanResponse = null;
        PriceResponse priceResponse = null;
        try {
            Utils.addCMRequest(priceRequest, transactionLog);
            for (UpdatePriceOTA updatePriceOTA : buildUpdatePriceRequests(priceRequest)) {
                String jsonString = MarshalUnmarshalUtils.serializeWithPropertyNaming(updatePriceOTA);
                log.info("update price request:: " + jsonString);
                Utils.addOTARequest(jsonString, transactionLog);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(jsonString, httpHeaders);
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(getUpdatePriceUrl, entity, String.class);
                String response = responseEntity.getBody();
                log.info("Response for update price....." + response);
                Utils.addOTAResponse(response, transactionLog);
                PriceUpdateResponse priceUpdateResponse = MarshalUnmarshalUtils.deserialize(response, PriceUpdateResponse.class);
                priceResponse = buildPriceResponse(priceUpdateResponse);
                Utils.addCMResponse(priceResponse, transactionLog);
            }
        } catch (Throwable throwable) {
            Utils.addOTAResponse(throwable, transactionLog);
            throw throwable;
        } finally {
            repository.save(transactionLog);
        }
        return priceResponse;
    }

    @Override
    public RestrictionResponse updateRestriction(RestrictionRequest restrictionRequest) throws Exception {
        TransactionLog transactionLog = new TransactionLog();
        Utils.addCommonData(restrictionRequest, transactionLog);
        RestrictionResponse restrictionResponse = null;
        try {
            Utils.addCMRequest(restrictionRequest, transactionLog);
            List<RestrictionUpdate> restrictionUpdateRequests = buildRestrictionUpdateRequest(restrictionRequest);
            for (RestrictionUpdate eachRestrictionUpdate : restrictionUpdateRequests) {
                String jsonString = MarshalUnmarshalUtils.serialize(eachRestrictionUpdate);
                log.info("update restriction request:: " + jsonString);

                Utils.addOTARequest(jsonString, transactionLog);
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(jsonString, httpHeaders);
                ResponseEntity<String> responseEntity = restTemplate
                        .postForEntity(getUpdateRestrictionUrl, entity, String.class);
                String response= responseEntity.getBody();
                log.info("Update restrcition Response FROM OTA" + response);
                Utils.addOTAResponse(response, transactionLog);
                RestrictionUpdateResponse restrictionUpdateResponse = MarshalUnmarshalUtils.deserialize(response, RestrictionUpdateResponse.class);
                restrictionResponse = buildRestrictionResponse(restrictionUpdateResponse);
                Utils.addCMResponse(restrictionResponse, transactionLog);
            }

        } catch (Throwable throwable) {
            log.error(throwable.toString(), throwable);
            Utils.addOTAResponse(throwable, transactionLog);
            throw throwable;
        } finally {
            repository.save(transactionLog);
        }
        return restrictionResponse;
    }

    /*
    Because common format has support for single room per request as of now
     */
    private List<InventoryUpdate> buildInventoryUpdateRequest(InventoryRequest inventoryRequest) {
        List<InventoryUpdate> inventoryUpdates = new ArrayList<InventoryUpdate>();
        for (InventoryData eachInventoryData : inventoryRequest.getData()) {
            InventoryUpdate inventoryUpdate = new InventoryUpdate();
            inventoryUpdates.add(inventoryUpdate);
            Auth auth = new Auth();
            inventoryUpdate.setAuth(auth);
            auth.setKey(apiKey);
            Data data = new Data();
            inventoryUpdate.setData(data);
            data.setPropertyId(inventoryRequest.getHotelId());
            data.setRoomId(eachInventoryData.getRoomId());
            List<com.axisrooms.homeyhuts.generated.updateInventory.Inventory> inventories = new ArrayList<>();
            data.setInventory(inventories);
            for (Inventory eachInventory : eachInventoryData.getInventories()) {
                com.axisrooms.homeyhuts.generated.updateInventory.Inventory inventory = new com.axisrooms.homeyhuts.generated.updateInventory.Inventory();
                inventories.add(inventory);
                inventory.setEndDate(eachInventory.getEndDate());
                inventory.setStartDate(eachInventory.getStartDate());
                inventory.setFree(eachInventory.getInventory());
            }
        }
        return inventoryUpdates;
    }

    private InventoryResponse buildInventoryResponse(InventoryUpdateResponse inventoryUpdateResponse) {
        InventoryResponse inventoryResponse = new InventoryResponse();
        if (inventoryUpdateResponse.getstatus().equalsIgnoreCase("success")) {
            inventoryResponse.setMessage("success");
            inventoryResponse.setHttpStatusCode(HttpStatus.OK.value());
        } else {
            inventoryResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            inventoryResponse.setMessage(inventoryUpdateResponse.getmessage());
        }
        return inventoryResponse;
    }

    private PriceResponse buildPriceResponse(PriceUpdateResponse priceUpdateResponse) {
        PriceResponse priceResponse = new PriceResponse();
        if (priceUpdateResponse.getstatus().equalsIgnoreCase("success")) {
            priceResponse.setMessage("success");
            priceResponse.setHttpStatusCode(HttpStatus.OK.value());
        } else {
            priceResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            priceResponse.setMessage(priceUpdateResponse.getmessage());
        }
        return priceResponse;
    }

    private List<UpdatePriceOTA> buildUpdatePriceRequests(PriceRequest priceRequest)
            throws OccupancyNotSupportedException {
        List<UpdatePriceOTA> updatePriceOTAList = new ArrayList<>();
        String hotelId = priceRequest.getHotelId();
        for (PriceData eachPriceData : priceRequest.getData()) {
            for (RoomDetail roomDetail : eachPriceData.getRoomDetails()) {
                for (RatePlanDetail ratePlanDetail : roomDetail.getRatePlanDetails()) {
                    try {
                        for (com.axisrooms.homeyhuts.bean.Rate eachRate : ratePlanDetail.getRates()) {
                            //multiple apis here
                            UpdatePriceOTA updatePriceOTA = new UpdatePriceOTA();
                            updatePriceOTAList.add(updatePriceOTA);
                            com.axisrooms.homeyhuts.generated.updatePrice.Auth auth = new com.axisrooms.homeyhuts.generated.updatePrice.Auth();
                            updatePriceOTA.setauth(auth);
                            auth.setkey(apiKey);
                            com.axisrooms.homeyhuts.generated.updatePrice.Data data = new com.axisrooms.homeyhuts.generated.updatePrice.Data();
                            updatePriceOTA.setdata(data);
                            data.setpropertyId(hotelId);
                            data.setroomId(roomDetail.getRoomId());
                            data.setrateplanId(ratePlanDetail.getRatePlanId());
                            List<Rate> rates = new ArrayList<>();
                            data.setrate(rates);
                            Map<String, String> priceMap = eachRate.getPrices();
                            Rate rate = new Rate();
                            rates.add(rate);
                            rate.setendDate(eachRate.getEndDate());
                            rate.setstartDate(eachRate.getStartDate());
                            for (String occupancy : priceMap.keySet()) {
                                rate.setPriceByOccupancyName(occupancy, priceMap.get(occupancy));
                            }
                        }
                    } catch (OccupancyNotSupportedException e) {
                        log.error("Exception while preparing price update api input:: " + e.getMessage());
                        throw e;
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        throw e;
                    }
                }
            }

        }
        return updatePriceOTAList;
    }

    private RatePlanResponse buildRatePlanResponse(RateUpdateResponse rateUpdateResponse) {
        RatePlanResponse ratePlanResponse = new RatePlanResponse();
        if (rateUpdateResponse.getStatus().equalsIgnoreCase("success")) {
            ratePlanResponse.setMessage("success");
            ratePlanResponse.setHttpStatusCode(HttpStatus.OK.value());
        } else {
            ratePlanResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            ratePlanResponse.setMessage(ratePlanResponse.getMessage());
        }
        return ratePlanResponse;
    }

    private ProductInfoRequest buildProductInfoRequest(String hotelId) {
        ProductInfoRequest productInfoRequest = new ProductInfoRequest();
        com.axisrooms.homeyhuts.generated.productInfo.Auth auth = new com.axisrooms.homeyhuts.generated.productInfo.Auth();
        productInfoRequest.setAuth(auth);
        auth.setKey(apiKey);
        productInfoRequest.setPropertyId(hotelId);
        return productInfoRequest;
    }

    private ProductInfoResponse getProductInfo(ProductInfoRequest productInfoRequest) throws Exception {
        String jsonRequest = MarshalUnmarshalUtils.serialize(productInfoRequest);
        log.info("Input request to fetch rooms Request:-> " + jsonRequest);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, httpHeaders);
        String response = restTemplate.postForObject(getRoomsUrl, entity, String.class);
        try{
            return MarshalUnmarshalUtils.deserialize(response, ProductInfoResponse.class);
        }catch(Exception e){
            log.info(e.getMessage());
        }
        finally {
            return MarshalUnmarshalUtils.deserialize(response, ProductInfoResponse.class);
        }
    }


    /*
    axisagent-commonOta conversion
     */
    private RoomResponse buildRoomResponse(ProductInfoResponse productInfoResponse) throws Exception {
        RoomResponse roomResponse = new RoomResponse();
        if (Objects.nonNull(productInfoResponse)) {
            log.info("productInfoResponse - " + productInfoResponse);
            if (productInfoResponse.getStatus().contains("success") || productInfoResponse.getStatus().contains("Success")) {
                Set<Description> descriptions = new HashSet<>();
                for (Datum datum : productInfoResponse.getData()) {
                    Description description = new Description();
                    descriptions.add(description);
                    description.setId(datum.getId());
                    description.setName(datum.getName());
                }
                roomResponse.setDescriptions(descriptions);
                roomResponse.setMessage(SUCCESS);
                roomResponse.setHttpStatusCode(HttpStatus.OK.value());
            } else {
                roomResponse
                        .setMessage("request to fetch rooms failed from ota::  " + productInfoResponse.getMessage());
                roomResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            roomResponse.setMessage("Marshaling/Serialization Exception Occured.");
            roomResponse.setHttpStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
        }
        return roomResponse;
    }

    private RatePlanInfoRequest buildRatePlanInfoRequest(String hotelId, String roomId) {
        RatePlanInfoRequest ratePlanInfoRequest = new RatePlanInfoRequest();
        com.axisrooms.homeyhuts.generated.RatePlanInfo.Auth auth = new com.axisrooms.homeyhuts.generated.RatePlanInfo.Auth();
        ratePlanInfoRequest.setAuth(auth);
        auth.setKey(apiKey);
        ratePlanInfoRequest.setPropertyId(hotelId);
        ratePlanInfoRequest.setRoomId(roomId);
        return ratePlanInfoRequest;
    }

    private RatePlanInfoResponse getRatePlanInfo(RatePlanInfoRequest ratePlanInfoRequest) throws Exception {
//        String jsonRequest = MarshalUnmarshalUtils.serialize(ratePlanInfoRequest);
        String jsonRequest = new ObjectMapper().writeValueAsString(ratePlanInfoRequest);
        log.info("Input request to fetch rate plans: -> " + jsonRequest);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, httpHeaders);
        String response = "";
//        response = restTemplate.postForObject(getRoomsUrl, entity, String.class);  use this URL for getRooms
        response = restTemplate.postForObject(getProductInfoUrl, entity, String.class);//use this for getRatePlans
        log.info("Axisrooms ota get rate plans Response=" + response);
        return MarshalUnmarshalUtils.deserialize(response, RatePlanInfoResponse.class);
    }

    private RatePlanResponse buildRatePlansResponse(RatePlanInfoResponse ratePlanInfoResponse, String roomId) {
        RatePlanResponse ratePlanResponse = new RatePlanResponse();
        if (Objects.nonNull(ratePlanInfoResponse)) {
            if (ratePlanInfoResponse.getStatus().equalsIgnoreCase("success")) {
                ratePlanResponse.setHttpStatusCode(HttpStatus.OK.value());
                ratePlanResponse.setMessage("success");
                List<RatePlanDescription> ratePlanDescriptions = new ArrayList<>();
                ratePlanResponse.setRatePlanDescriptions(ratePlanDescriptions);
                RatePlanDescription ratePlanDescription = new RatePlanDescription();
                ratePlanDescriptions.add(ratePlanDescription);
                ratePlanDescription.setRoomId(roomId);
                List<RatePlanConfiguration> ratePlanConfigurations = new ArrayList<>();
                ratePlanDescription.setConfigurations(ratePlanConfigurations);
                for (com.axisrooms.homeyhuts.generated.RatePlanInfo.Datum datum : ratePlanInfoResponse.getData()) {
                    RatePlanConfiguration ratePlanConfiguration = new RatePlanConfiguration();
                    ratePlanConfigurations.add(ratePlanConfiguration);
                    if (datum.getCommissionPerc() != null) {
                        ratePlanConfiguration.setCommission(datum.getCommissionPerc());
                    }
                    if (datum.getRatePlanId() != null) {
                        ratePlanConfiguration.setRatePlanId(datum.getRatePlanId());
                    }
                    // ratePlanConfiguration.setRatePlanId("653");
                    log.info("rateplanId----------" + ratePlanConfiguration.getRatePlanId());
                    if (datum.getRatePlanName() != null && datum.getRatePlanName() != "") {
                        ratePlanConfiguration.setRatePlanName(datum.getRatePlanName());
                        log.info("ratePlanName if non null or non empty-----" + ratePlanConfiguration.getRatePlanName());
                    } else {
                        ratePlanConfiguration.setRatePlanName(datum.getRatePlanId());
                        log.info("rateplanName if else------------" + ratePlanConfiguration.getRatePlanName());
                    }
                    if (datum.getTaxPerc() != null) {
                        ratePlanConfiguration.setTax(datum.getTaxPerc());
                        log.info("tax------" + ratePlanConfiguration.getTax());
                    }
                    if (datum.getValidity() != null) {
                        Validity validity = datum.getValidity();
                        RatePlanValidity ratePlanValidity = new RatePlanValidity();
                        ratePlanValidity.setStartDate(validity.getStartDate());
                        ratePlanValidity.setEndDate(validity.getEndDate());
                        ratePlanConfiguration.setValidityList(new ArrayList<RatePlanValidity>(Arrays.asList(ratePlanValidity)));
                    }
                    if (datum.getValidity() == null) {
                        RatePlanValidity ratePlanValidity = new RatePlanValidity();
                        ratePlanValidity.setStartDate(LocalDate.parse("2024-05-28"));
                        ratePlanValidity.setEndDate(LocalDate.parse("2029-05-28"));
                        ratePlanConfiguration.setValidityList(new ArrayList<>(Arrays.asList(ratePlanValidity)));
                    }
                    ratePlanDescription.setCurrency(datum.getCurrency());
                    List<String> occupancies = new ArrayList<>();
                    ratePlanDescription.setOccupancies(occupancies);

                    for (String occupancy : datum.getOccupancy()) {
                        occupancies.add(occupancy);
                    }
                }
            } else {
                ratePlanResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                ratePlanResponse.setMessage("External api call failed " + ratePlanInfoResponse.getMessage());
            }
        } else {
            ratePlanResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            ratePlanResponse.setMessage("Marshalling error occurred");
        }
        return ratePlanResponse;
    }

    //new code

    private List<RestrictionUpdate> buildRestrictionUpdateRequest(RestrictionRequest restrictionRequest) throws JsonProcessingException {
        log.info("In buildRestrictionUpdateRequest method---------");
        List<RestrictionUpdate> restrictionUpdates = new ArrayList<RestrictionUpdate>();
        for (RestrictionData eachRestrictionData : restrictionRequest.getData()) {
            RestrictionUpdate restrictionUpdate = new RestrictionUpdate();
            restrictionUpdates.add(restrictionUpdate);
            com.axisrooms.homeyhuts.generated.updateRestriction.Auth auth = new com.axisrooms.homeyhuts.generated.updateRestriction.Auth();
            restrictionUpdate.setAuth(auth);
            auth.setKey(apiKey);
            List<com.axisrooms.homeyhuts.generated.updateRestriction.Data> data = new ArrayList<>();
            restrictionUpdate.setData(data);
            for(com.axisrooms.homeyhuts.bean.RestrictionData eachData:restrictionRequest.getData()){
                com.axisrooms.homeyhuts.generated.updateRestriction.Data datas = new com.axisrooms.homeyhuts.generated.updateRestriction.Data();
                data.add(datas);
                datas.setPropertyId(restrictionRequest.getHotelId());
                List<com.axisrooms.homeyhuts.generated.updateRestriction.RoomDetails> roomDetails = new ArrayList<>();
                datas.setRoomDetails(roomDetails);
                for (RoomDetail eachRoomDetail : eachRestrictionData.getRoomDetails()) {
                    com.axisrooms.homeyhuts.generated.updateRestriction.RoomDetails roomDetail = new com.axisrooms.homeyhuts.generated.updateRestriction.RoomDetails();
                    roomDetails.add(roomDetail);
                    roomDetail.setRoomId(eachRoomDetail.getRoomId());
                    List<com.axisrooms.homeyhuts.generated.updateRestriction.RatePlanDetails> ratePlanDetails = new ArrayList<>();
                    roomDetail.setRatePlanDetails(ratePlanDetails);
                    for (RatePlanDetail eachRateplanDetail : eachRoomDetail.getRatePlanDetails()) {
                        com.axisrooms.homeyhuts.generated.updateRestriction.RatePlanDetails rateplanDetail = new com.axisrooms.homeyhuts.generated.updateRestriction.RatePlanDetails();
                        ratePlanDetails.add(rateplanDetail);
                        rateplanDetail.setRatePlanId(eachRateplanDetail.getRatePlanId());
                        com.axisrooms.homeyhuts.generated.updateRestriction.Restrictions restrictionsDetails = new Restrictions();
                        rateplanDetail.setRestrictions(restrictionsDetails);
                        if(eachRateplanDetail.getRestrictions().getType().equalsIgnoreCase("COA") || eachRateplanDetail.getRestrictions().getType().equalsIgnoreCase("COD")){
                            if(eachRateplanDetail.getRestrictions().getValue().equalsIgnoreCase("true")){
                                restrictionsDetails.setValue("Close");
                            } else {
                                restrictionsDetails.setValue("Open");
                            }
                        } else {
                            restrictionsDetails.setValue(eachRateplanDetail.getRestrictions().getValue());
                        }
                        restrictionsDetails.setType(eachRateplanDetail.getRestrictions().getType());
                        if (restrictionsDetails.getType().equals("MLOS")){
                            restrictionsDetails.setType("MLos");
                        }
                        List<com.axisrooms.homeyhuts.generated.updateRestriction.Periods> periods = new ArrayList<>();
                        restrictionsDetails.setPeriods(periods);
                        for (Period eachPeriod : eachRateplanDetail.getRestrictions().getPeriods()) {
                            com.axisrooms.homeyhuts.generated.updateRestriction.Periods periodsDetail = new com.axisrooms.homeyhuts.generated.updateRestriction.Periods();
                            periods.add(periodsDetail);
                            periodsDetail.setStartDate(eachPeriod.getStartDate());
                            periodsDetail.setEndDate(eachPeriod.getEndDate());
                        }

                        //adding another rateplan for maxlos
                        if(eachRateplanDetail.getRestrictions().getType().equalsIgnoreCase("MLOS")
                                && (Objects.nonNull(eachRateplanDetail.getRestrictions().getMaxLOSValue()) && !eachRateplanDetail.getRestrictions().getMaxLOSValue().equalsIgnoreCase("0"))){
                            com.axisrooms.homeyhuts.generated.updateRestriction.RatePlanDetails ratePlanDetailsMAXLOS = new com.axisrooms.homeyhuts.generated.updateRestriction.RatePlanDetails();
                            ratePlanDetails.add(ratePlanDetailsMAXLOS);
                            ratePlanDetailsMAXLOS.setRatePlanId(rateplanDetail.getRatePlanId());
                            com.axisrooms.homeyhuts.generated.updateRestriction.Restrictions restrictionsDetailsMAXLOS = new Restrictions();
                            ratePlanDetailsMAXLOS.setRestrictions(restrictionsDetailsMAXLOS);
                            restrictionsDetailsMAXLOS.setPeriods(rateplanDetail.getRestrictions().getPeriods());
                            restrictionsDetailsMAXLOS.setType("MaxLos");
                            restrictionsDetailsMAXLOS.setValue(eachRateplanDetail.getRestrictions().getMaxLOSValue());
                        }

                    }
                }
            }
        }
        return restrictionUpdates;
    }



    private RestrictionResponse buildRestrictionResponse(RestrictionUpdateResponse restrictionUpdateResponse) {
        RestrictionResponse restrictionResponse = new RestrictionResponse();
        if (restrictionUpdateResponse.getStatus().equalsIgnoreCase("success")) {
            restrictionResponse.setMessage("success");
            restrictionResponse.setHttpStatusCode(HttpStatus.OK.value());
        } else {
            restrictionResponse.setHttpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            restrictionResponse.setMessage(restrictionUpdateResponse.getMessage());
        }
        return restrictionResponse;
    }
}