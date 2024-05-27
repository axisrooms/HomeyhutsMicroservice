package com.axisrooms.homeyhuts.manager;

import com.axisrooms.homeyhuts.request.InventoryRequest;
import com.axisrooms.homeyhuts.request.PriceRequest;
import com.axisrooms.homeyhuts.request.RestrictionRequest;
import com.axisrooms.homeyhuts.response.*;
import com.axisrooms.homeyhuts.util.OccupancyNotSupportedException;
import org.springframework.stereotype.Service;

@Service
public interface OTAManager {
    RoomResponse getRoomList(String hotelId) throws Exception;

    RatePlanResponse getRatePlans(String hotelId, String roomId) throws Exception;

    InventoryResponse updateInventory(InventoryRequest inventoryRequest) throws Exception;

    PriceResponse updatePrice(PriceRequest priceRequest) throws OccupancyNotSupportedException,Exception;

    RestrictionResponse updateRestriction(RestrictionRequest restrictionRequest) throws Exception;
}
