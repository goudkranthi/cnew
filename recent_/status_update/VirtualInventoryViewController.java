package com.capturerx.cumulus4.virtualinventory.controllers;


import com.capturerx.common.errors.InvalidRequestException;
import com.capturerx.cumulus4.virtualinventory.dtos.*;
import com.capturerx.cumulus4.virtualinventory.errors.ErrorConstants;
import com.capturerx.cumulus4.virtualinventory.errors.InternalServerException;
import com.capturerx.cumulus4.virtualinventory.models.ReplenishmentVirtualInv;
import com.capturerx.cumulus4.virtualinventory.services.VirtualInventoryViewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@Api(value = "/cumulus/v1", description = "Operations related to Virtual Inventory Ndc Events")
@RequestMapping(path = "/cumulus/v1")
@CrossOrigin(origins = "*")
public class VirtualInventoryViewController extends BaseController{

    private final String DATEFORMAT = "yyyy-MM-dd";
    @Autowired
    private VirtualInventoryViewService virtualInventoryViewService;

    @PostMapping(value = "/getallndcs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get All Ndcs that are present for the header", notes = "Retrieving all the Ndc's for the header", response = VirtualInventoryNdcUiViewDTO[].class)
    @PreAuthorize("hasRole(T(com.capturerx.cumulus4.virtualinventory.configuration.RolesList).C4_CANREADVIRTUALINVENTORY.toString())")
    public ResponseEntity<List<VirtualInventoryNdcUiViewDTO>> getAllNdcs(
            @RequestBody
            VirtualInventoryNdcUiViewDTO virtualInventoryNdcUiViewDTO){
        List<VirtualInventoryNdcUiViewDTO> virtualInventoryNdcUiViewDTOS = virtualInventoryViewService.getNdcsByBillToAndShipTo(virtualInventoryNdcUiViewDTO);
        if(null == virtualInventoryNdcUiViewDTOS || virtualInventoryNdcUiViewDTOS.isEmpty())
            return generateResourceGetAllNoContentResponse(virtualInventoryNdcUiViewDTOS);
        else
            return generateResourceGetAllResponseOK(virtualInventoryNdcUiViewDTOS);
    }

    @GetMapping(path = "/getevents/{ndcId}/{eventAsOfDate}")
    @ApiOperation(value = "Get All events for the give ndc and event as of date", notes = "Retrieve the top 500 events for that ndc and event as of date", response = VirtualInventoryEventsViewDTO[].class)
    @PreAuthorize("hasRole(T(com.capturerx.cumulus4.virtualinventory.configuration.RolesList).C4_CANREADVIRTUALINVENTORY.toString())")
    public ResponseEntity<List<VirtualInventoryEventsViewDTO>> getEventsbyNdcId(
            @Valid
            @PathVariable("ndcId")
            @ApiParam(required = true, name = "ndcId", value = "events identifier by ndc")
                    UUID ndcId,
            @Valid
            @PathVariable("eventAsOfDate")
                    String eventAsOfDate){
        DateTimeFormatter dateTimeFormatter;
        DateTime dateTime;
        try{
            dateTimeFormatter = DateTimeFormat.forPattern(DATEFORMAT);
            dateTime = dateTimeFormatter.parseDateTime(eventAsOfDate);
        } catch (Exception ex) {
            throw new InvalidRequestException(String.format(ErrorConstants.VI_EVENT_BAD_DATE, eventAsOfDate));
        }
        try {
            List<VirtualInventoryEventsViewDTO> virtualInventoryEventsViewDTOS = virtualInventoryViewService.getEventsByNdcIdAndEventAsOfDate(ndcId, dateTime.toLocalDateTime().toDateTime(DateTimeZone.UTC));
            if(virtualInventoryEventsViewDTOS == null || virtualInventoryEventsViewDTOS.isEmpty())
                return generateResourceGetAllNoContentResponse(virtualInventoryEventsViewDTOS);
            else
                return generateResourceGetAllResponseOK(virtualInventoryEventsViewDTOS);
        } catch (Exception ex) {
            throw new InternalServerException(ErrorConstants.GET_EVENTS_EXCEPTION);
        }
    }

    @ApiOperation(value = "Add Virtual Inventory Ndc Events for Orders", notes = "Create new vi drug events for orders", response = VirtualInventoryNdcEventDTO[].class)
    @GetMapping(path = "/contracts/headers/{shipToId}")
    public List<ReplenishmentVirtualInv> getContractsAndHeadersByShipToId(
            @Valid
            @PathVariable("shipToId")
                    UUID shipToId){
        return virtualInventoryViewService.getContractsAndHeaders(shipToId);
    }

    @ApiOperation(value = "Retrieve the contract, header and ndc information for the given ship to id ", notes = "VI details and contract details for the given ship to id", response = ReplenishmentVIDTO[].class)
    @GetMapping(path = "/vi/replenish/{shipToId}")
    public ResponseEntity<List<ReplenishmentVIDTO>> getContractsAndNdcsByShipToId(
            @Valid
            @PathVariable("shipToId")
                    UUID shipToId){
        List<ReplenishmentVIDTO> replenishmentVIDTOS = virtualInventoryViewService.getContractAndNdcByShipToId(shipToId);
        if(replenishmentVIDTOS == null || replenishmentVIDTOS.isEmpty())
            return generateResourceGetAllNoContentResponse(replenishmentVIDTOS);
        else
            return generateResourceGetAllResponseOK(replenishmentVIDTOS);
    }

    @ApiOperation(value = "Receive batch id and contract ids from billing", notes = "VI receiving batch and contract details from billing", response = BillingDTO[].class)
    @PostMapping(path = "/vi/billing")
    public ResponseEntity postBatchIdAndContractIds(
            @RequestBody BillingDTO billingDTOS){
        if (null == billingDTOS || null == billingDTOS.getBatchId() ||
                null == billingDTOS.getContractIds() || billingDTOS.getContractIds().isEmpty()) {
            throw new InvalidRequestException("Batch Id and/or contract ids are null");
        }
        virtualInventoryViewService.publishContractDetailsToTrueUp(billingDTOS);
        return ResponseEntity.status(HttpStatus.OK).body("Post Call Successful");
    }

    @ApiOperation(value = "Add or Update the received batch id and contract ids from billing", notes = "VI receiving batch and contract details from billing and updates the TU status table", response = BillingDTO[].class)
    @PostMapping(path = "/vi/billing/trueUpStatus")
    public ResponseEntity persistTrueUpStatus (
            @RequestBody BillingDTO billingDTOS){
        if (null == billingDTOS || null == billingDTOS.getBatchId() ||
                null == billingDTOS.getContractIds() || billingDTOS.getContractIds().isEmpty()) {
            throw new InvalidRequestException("Batch Id and/or contract ids are null");
        }
        virtualInventoryViewService.persistTrueUpStatus(billingDTOS);
        return ResponseEntity.status(HttpStatus.OK).body("Post Call Successful");
    }
}
