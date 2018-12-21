package com.capturerx.cumulus4.virtualinventory.services;


import com.capturerx.cumulus4.virtualinventory.dtos.*;
import com.capturerx.cumulus4.virtualinventory.models.ReplenishmentVirtualInv;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface VirtualInventoryViewService {
    List<VirtualInventoryEventsViewDTO> getEventsByNdcIdAndEventAsOfDate(UUID ndcId, DateTime eventAsOfDate);

    List<VirtualInventoryNdcUiViewDTO> getNdcsByBillToAndShipTo(VirtualInventoryNdcUiViewDTO virtualInventoryNdcUiViewDTO);

    List<ReplenishmentVirtualInv> getContractsAndHeaders(UUID shipToId);

    Boolean wholesalerAccountExists(UUID billToId, UUID shipToId);

    List<ReplenishmentVIDTO> getContractAndNdcByShipToId(UUID shipToId);

    void publishContractDetailsToTrueUp(BillingDTO billingDTO);

    void persistTrueUpStatus(BillingDTO billingDTO);

}