package com.capturerx.cumulus4.virtualinventory.impls;


import com.capturerx.common.errors.InvalidRequestException;
import com.capturerx.common.errors.PreConditionFailedException;
import com.capturerx.cumulus4.virtualinventory.configuration.VirtualInventorySecurityConfiguration;
import com.capturerx.cumulus4.virtualinventory.dtos.*;
import com.capturerx.cumulus4.virtualinventory.errors.AccessForbidenException;
import com.capturerx.cumulus4.virtualinventory.errors.ErrorConstants;
import com.capturerx.cumulus4.virtualinventory.errors.InternalServerException;
import com.capturerx.cumulus4.virtualinventory.message.constants.MessageChannelConstants;
import com.capturerx.cumulus4.virtualinventory.message.constants.MessageConstants;
import com.capturerx.cumulus4.virtualinventory.message.constants.MessageExceptionConstants;
import com.capturerx.cumulus4.virtualinventory.message.utils.BillingTrueUpMessageUtils;
import com.capturerx.cumulus4.virtualinventory.models.*;
import com.capturerx.cumulus4.virtualinventory.repositories.*;
import com.capturerx.cumulus4.virtualinventory.services.VirtualInventoryViewService;
import com.capturerx.cumulus4.virtualinventory.services.mappers.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VirtualInventoryViewServiceImpl extends MessageService implements VirtualInventoryViewService {

    private static final Logger logger = LoggerFactory.getLogger(VirtualInventoryViewServiceImpl.class);

    @PersistenceContext
    private final EntityManager entityManager;

    @Value("${ndc.size}")
    private int ndcSize;

    @Value("${apiservices.path}")
    private String apiPath;


    private final VirtualInventorySecurityConfiguration virtualInventorySecurityConfiguration;

    private final RestTemplate restTemplate;

    private static final String APPROVED = "Approved";
    private static final String PREEXIST = "PreExist";
    private static final String REVERSED = "Reversed";
    private static final String REVERSE_STABILIZER = "Reverse Stabilizer";
    private static final String ORDER_IMPACT = "OrderImpact";
    private static final String ORDERED = "Ordered";
    private static final String UNDER_REPLENISHMENT = "Under Replenishment";
    private static final String ACKNOWLEDGEMENT = "Acknowledgement";
    private static final String BACKFILL = "BackFill";
    private static final String STABILIZER = "Stabilizer";
    private static final String MANUAL_ADJUSTMENT = "Manual Adjustment";
    private static final String PREBUY = "PreBuy";
    private static final String OVER_REPLENISHMENT = "Over Replenishment";
    private static final String BILL_TO_ID = "billToId";
    private static final String SHIP_TO_ID = "shipToId";
    private static final String API_BILL_TO_ID = "billtoid";
    private static final String API_SHIP_TO_ID = "shiptoid";
    private static final String NDC = "ndc";
    private static final String PACKAGE_AVAILABLE_TO_ORDER="packageQuantity";
    private static final String LAST_CALCULATED_DATE ="lastCalculatedDate";
    private static final String DRUG_DESCRIPTION ="drugDescription";
    private static final String PERCENTAGE = "%";
    private static final String EMPTY = "";
    private static final String ENTITY = "entity";
    private static final String BUILDER = "builder";
    private static final String urlPath = "secureusers/user/";
    private static final SimpleGrantedAuthority C4_INTERNAL = new SimpleGrantedAuthority("ROLE_C4_INTERNAL");


    private final VirtualInventoryNdcViewRepository virtualInventoryNdcViewRepository;
    private final VirtualInventoryEventsViewRepository virtualInventoryEventsViewRepository;
    private final VirtualInventoryNdcViewMapper virtualInventoryNdcViewMapper;
    private final VirtualInventoryEventsViewMapper virtualInventoryEventsViewMapper;
    private final ReplenishmentNDCMapper replenishmentNDCMapper;
    private final VirtualInventoryTrueUpStatusRepository virtualInventoryTrueUpStatusRepository;

    public VirtualInventoryViewServiceImpl(EntityManager entityManager, VirtualInventorySecurityConfiguration virtualInventorySecurityConfiguration, RestTemplate restTemplate, VirtualInventoryNdcViewRepository virtualInventoryNdcViewRepository, VirtualInventoryEventsViewRepository virtualInventoryEventsViewRepository, VirtualInventoryNdcViewMapper virtualInventoryNdcViewMapper, VirtualInventoryEventsViewMapper virtualInventoryEventsViewMapper, ReplenishmentNDCMapper replenishmentNDCMapper, VirtualInventoryTrueUpStatusRepository virtualInventoryTrueUpStatusRepository) {
        this.entityManager = entityManager;
        this.virtualInventorySecurityConfiguration = virtualInventorySecurityConfiguration;
        this.restTemplate = restTemplate;
        this.virtualInventoryNdcViewRepository = virtualInventoryNdcViewRepository;
        this.virtualInventoryEventsViewRepository = virtualInventoryEventsViewRepository;
        this.virtualInventoryNdcViewMapper = virtualInventoryNdcViewMapper;
        this.virtualInventoryEventsViewMapper = virtualInventoryEventsViewMapper;
        this.replenishmentNDCMapper = replenishmentNDCMapper;
        this.virtualInventoryTrueUpStatusRepository = virtualInventoryTrueUpStatusRepository;
    }

    @Override
    public Boolean wholesalerAccountExists(UUID billToId, UUID shipToId){
        ResponseEntity<WholesalerAccountDTO[]> wholesalerAccountDTOResponseEntity = null;
        try {
            String wsauri = "wholesaler_accounts/billtoid/{billToId}/shiptoid/{shipToId}";
            Map wsaInfo = new HashMap();
            wsaInfo.put(BILL_TO_ID, billToId);
            wsaInfo.put(SHIP_TO_ID, shipToId);
            Map entityMap = virtualInventorySecurityConfiguration.getEntity(apiPath, wsauri, wsaInfo);
            if (null != entityMap && null != entityMap.get(BUILDER) && null != entityMap.get(ENTITY)) {
                wholesalerAccountDTOResponseEntity = restTemplate.exchange(entityMap.get(BUILDER).toString(), HttpMethod.GET,
                            (HttpEntity) entityMap.get(ENTITY), WholesalerAccountDTO[].class);

                if (null != wholesalerAccountDTOResponseEntity && wholesalerAccountDTOResponseEntity.getStatusCode() == HttpStatus.OK) {
                    List<WholesalerAccountDTO> wholesalerAccountDTOList = Arrays.asList(wholesalerAccountDTOResponseEntity.getBody());
                    if (null != wholesalerAccountDTOList && !wholesalerAccountDTOList.isEmpty() && wholesalerAccountDTOList.size() > 0) {
                        return true;
                    }
                }
            } else {
                throw new InternalServerException(ErrorConstants.FAILED_TO_PREPARE_ENTITY);
            }
        }  catch (HttpClientErrorException httpException) {
            logger.error(ErrorConstants.HTTP_EXCEPTION_WHOLESALER_ACCOUNT_EXIST, httpException);
        }catch (Exception ex) {
            logger.error(ErrorConstants.EXCEPTION_WHOLESALER_ACCOUNT_EXIST, ex);
        }
        return false;
    }

    @Override
    public List<VirtualInventoryEventsViewDTO> getEventsByNdcIdAndEventAsOfDate(UUID ndcId, DateTime eventAsOfDate) {

            VirtualInventoryNdcUiView virtualInventoryNdc = virtualInventoryNdcViewRepository.findTopByVirtualInventoryNdcId(ndcId);
            if (null == virtualInventoryNdc) {
                throw new InvalidRequestException(String.format(ErrorConstants.VIRTUAL_INVENTORY_NDC_NOT_FOUND, ndcId));
            } else {
                if (wholesalerAccountExists(virtualInventoryNdc.getBillToId(), virtualInventoryNdc.getShipToId())) {
                    try {
                        List<VirtualInventoryEventsView> virtualInventoryEventsViews= virtualInventoryEventsViewRepository.findTop500ByNdcIdAndEventDateLessThanEqualAndEventTypeIsNotLikeOrderByEventDateDesc(ndcId, eventAsOfDate.plusDays(1), "Invoiced");
                        List<VirtualInventoryEventsViewDTO> virtualInventoryEventsViewDTOS = virtualInventoryEventsViews.stream().map(virtualInventoryEventsViewMapper:: toDto).collect(Collectors.toList());
                        List<VirtualInventoryEventsViewDTO> virtualInventoryEventsViewDTOS1 = new ArrayList<>();
                        for (VirtualInventoryEventsViewDTO virtualInventoryEventsViewDTO: virtualInventoryEventsViewDTOS){
                            if((PREEXIST.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (APPROVED.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (REVERSED.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (REVERSE_STABILIZER.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (ORDER_IMPACT.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType()))){
                                virtualInventoryEventsViewDTO.setReferenceNumber(true);
                                virtualInventoryEventsViewDTO.setOrderNumber(false);
                                virtualInventoryEventsViewDTO.setManualAdj(false);
                            }
                            else if((ORDERED.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (UNDER_REPLENISHMENT.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (ACKNOWLEDGEMENT.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (BACKFILL.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (STABILIZER.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType()))){
                                virtualInventoryEventsViewDTO.setReferenceNumber(true);
                                virtualInventoryEventsViewDTO.setOrderNumber(true);
                                virtualInventoryEventsViewDTO.setManualAdj(false);
                            }
                            else if((MANUAL_ADJUSTMENT.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType()))){
                                virtualInventoryEventsViewDTO.setReferenceNumber(false);
                                virtualInventoryEventsViewDTO.setOrderNumber(false);
                                virtualInventoryEventsViewDTO.setManualAdj(true);
                            }
                            else if((PREBUY.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType())) || (OVER_REPLENISHMENT.equalsIgnoreCase(virtualInventoryEventsViewDTO.getEventType()))){
                                virtualInventoryEventsViewDTO.setReferenceNumber(false);
                                virtualInventoryEventsViewDTO.setOrderNumber(true);
                                virtualInventoryEventsViewDTO.setManualAdj(false);
                            }
                            virtualInventoryEventsViewDTOS1.add(virtualInventoryEventsViewDTO);
                        }
                        return virtualInventoryEventsViewDTOS1;
                    } catch (Exception ex) {
                        throw new InternalServerException(ErrorConstants.GET_EVENTS_EXCEPTION + ' ' + ex);
                    }
                } else
                    throw new AccessForbidenException(String.format(ErrorConstants.ACCESS_DENIED, virtualInventoryNdc.getBillToId(), virtualInventoryNdc.getShipToId()));

            }

    }

    @Override
    public List<VirtualInventoryNdcUiViewDTO> getNdcsByBillToAndShipTo(VirtualInventoryNdcUiViewDTO virtualInventoryNdcUiViewDTO) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        List<VirtualInventoryNdcUiView> virtualInventoryNdcUiViews;
        CriteriaQuery query = builder.createQuery(VirtualInventoryNdcUiView.class);
        Root<VirtualInventoryNdcUiView> virtualInventoryNdcUiViewRoot = query.from(VirtualInventoryNdcUiView.class);
        List<Predicate> predicates = new ArrayList<>();
        if(wholesalerAccountExists(virtualInventoryNdcUiViewDTO.getBillToId(), virtualInventoryNdcUiViewDTO.getShipToId())) {
            predicates.add(builder.and(builder.equal(virtualInventoryNdcUiViewRoot.get(BILL_TO_ID), virtualInventoryNdcUiViewDTO.getBillToId())));
            predicates.add(builder.and(builder.equal(virtualInventoryNdcUiViewRoot.get(SHIP_TO_ID), virtualInventoryNdcUiViewDTO.getShipToId())));
            predicates.add(builder.and(builder.lessThanOrEqualTo(virtualInventoryNdcUiViewRoot.get(LAST_CALCULATED_DATE), virtualInventoryNdcUiViewDTO.getEventAsOfDate())));
            if (virtualInventoryNdcUiViewDTO.getNdc() != null) {
                predicates.add(builder.and(builder.equal(virtualInventoryNdcUiViewRoot.get(NDC), virtualInventoryNdcUiViewDTO.getNdc())));
            }
            if (virtualInventoryNdcUiViewDTO.isPackagesAvailableToOrder()) {
                predicates.add(builder.and(builder.greaterThan(virtualInventoryNdcUiViewRoot.get(PACKAGE_AVAILABLE_TO_ORDER), 0)));
            }
            String description = virtualInventoryNdcUiViewDTO.getDrugDescription();
            if (virtualInventoryNdcUiViewDTO.getDrugDescription() != null && !EMPTY.equalsIgnoreCase(description.trim())) {
                description = description.replace(" ", PERCENTAGE);
                predicates.add(builder.and(builder.like(builder.upper(virtualInventoryNdcUiViewRoot.get(DRUG_DESCRIPTION)), PERCENTAGE + description.toUpperCase() + PERCENTAGE)));
            }
            Predicate[] predicatesArray = new Predicate[predicates.size()];
            query.select(virtualInventoryNdcUiViewRoot).where(predicates.toArray(predicatesArray));
            virtualInventoryNdcUiViews = entityManager.createQuery(query).setMaxResults(ndcSize).getResultList();
            if (null == virtualInventoryNdcUiViews) {
                return null;
            }
        }
        else
            throw new AccessForbidenException(String.format(ErrorConstants.ACCESS_DENIED, virtualInventoryNdcUiViewDTO.getBillToId(), virtualInventoryNdcUiViewDTO.getShipToId()));
        return virtualInventoryNdcUiViews.stream().map(virtualInventoryNdcViewMapper::toDto).sorted(Comparator.comparing(VirtualInventoryNdcUiViewDTO::getEventAsOfDate).reversed()).collect(Collectors.toList());
    }

    public List<ContractsVirtualInventoryDTO> getContractsByShipToId(UUID shipToId){
        ResponseEntity<ContractsVirtualInventoryDTO[]> wholesalerAccountDTOResponseEntity = null;
        String contracturi = "vi_contracts/{shipToId}";
        Map shiptoInfo = new HashMap();
        shiptoInfo.put(SHIP_TO_ID, shipToId);
        Map entityMap = virtualInventorySecurityConfiguration.getEntity(apiPath, contracturi, shiptoInfo);
        try {
            if (shiptoInfo != null) {
                wholesalerAccountDTOResponseEntity = restTemplate.exchange(entityMap.get(BUILDER).toString(), HttpMethod.GET,
                        (HttpEntity) entityMap.get(ENTITY), ContractsVirtualInventoryDTO[].class);

                List<ContractsVirtualInventoryDTO> contractsVirtualInventories = Arrays.asList(wholesalerAccountDTOResponseEntity.getBody());
                if (null != contractsVirtualInventories && !contractsVirtualInventories.isEmpty()) {
                    return contractsVirtualInventories;
                }
            }
        }  catch (HttpClientErrorException httpException) {
            logger.error(ErrorConstants.HTTP_EXCEPTION_GET_BY_BILL_TO_AND_SHIP_TO, httpException);
        }catch (Exception ex) {
            logger.error(ErrorConstants.EXCEPTION_GET_BY_BILL_TO_AND_SHIP_TO, ex);
        }
        return null;
    }

    private List<VirtualInventoryNdcUiView> getAllNdcbyShipToPQtyGreaterthanZero(UUID shipToId) {
        List<VirtualInventoryNdcUiView> virtualInventoryNdcUiViews = virtualInventoryNdcViewRepository.findAllByShipToIdAndPackageQuantityGreaterThan(shipToId, 0);
        if (null == virtualInventoryNdcUiViews) return null;
        return virtualInventoryNdcUiViews;
    }

    @Override
    public List<ReplenishmentVIDTO> getContractAndNdcByShipToId(UUID shipToId) {
        try{
            List<ReplenishmentVIDTO> replenishmentVIDTOS = new ArrayList<>();
            List<ContractsVirtualInventoryDTO> contractsVirtualInventories = getContractsByShipToId(shipToId);
            if (null == contractsVirtualInventories) return null;
            List<VirtualInventoryNdcUiView> virtualInventoryNdcUiViews = getAllNdcbyShipToPQtyGreaterthanZero(shipToId);
            if (null == virtualInventoryNdcUiViews) return null;
            for(ContractsVirtualInventoryDTO contractsVirtualInventoryDTO: contractsVirtualInventories) {
                if (replenishmentVIDTOS.stream().noneMatch(r->(r.getBillToId().equals(contractsVirtualInventoryDTO.getBillToId())
                        && r.getShipToId().equals(contractsVirtualInventoryDTO.getShipToId())))) {
                    List<ReplenishmentVINdcDTO> replenishmentVINdcDTOS = new ArrayList<>();
                    ReplenishmentVIDTO replenishmentVIDTO = new ReplenishmentVIDTO();
                    replenishmentVIDTO.setContractId(contractsVirtualInventoryDTO.getContractId());
                    replenishmentVIDTO.setBillToId(contractsVirtualInventoryDTO.getBillToId());
                    replenishmentVIDTO.setBillToTypeCode(contractsVirtualInventoryDTO.getBillToTypeCode());
                    replenishmentVIDTO.setShipToId(contractsVirtualInventoryDTO.getShipToId());
                    replenishmentVIDTO.setShipToTypeCode(contractsVirtualInventoryDTO.getShipToTypeCode());
                    replenishmentVIDTO.setBillToName(contractsVirtualInventoryDTO.getBillToName());
                    replenishmentVIDTO.setShipToName(contractsVirtualInventoryDTO.getShipToName());
                    replenishmentVINdcDTOS = virtualInventoryNdcUiViews.stream()
                            .filter(c->(c.getShipToId().equals(shipToId) && c.getBillToId().equals(contractsVirtualInventoryDTO.getBillToId()))).map(replenishmentNDCMapper::toDto).collect(Collectors.toList());
                    replenishmentVIDTO.setNdcDTOList(replenishmentVINdcDTOS);
                    replenishmentVIDTOS.add(replenishmentVIDTO);
                }
            }
            return replenishmentVIDTOS;
        } catch (Exception ex) {
            throw new InternalServerException(String.format(ErrorConstants.VI_CONTRACT_REPLENISH_FAIL, shipToId));
        }
    }

    @Override
    public void publishContractDetailsToTrueUp(BillingDTO billingDTO) {
        Long batch = billingDTO.getBatchId();
        List<UUID> contracts = billingDTO.getContractIds();
        if (null != contracts && !contracts.isEmpty()) {
            for (UUID contractId: contracts) {
                if (null != contractId) {
                    Message message = MessageBuilder.withPayload(BillingTrueUpMessageUtils.generateTrueUpMessage(billingDTO.getBatchId(), contractId)).build();
                    try {
                        sendMessage(message, MessageChannelConstants.TRUE_UP_PRODUCER);
                        //DB saving -- first null values for status
                        //Alerady exist ( batchId and contractId ) - status update
                    } catch (Exception e) {
                        logger.error(MessageConstants.TRUE_UP + ":" + MessageExceptionConstants.TRUE_UP_ERR_MESSGAE ,e);
                    }
                }

            }
        }


    }

    // temporary method for Replenish
    private List<VirtualInventoryNdcUiViewDTO> getAllByShipToIdAndPackageQuantityGreaterThanZero(UUID shipToId) {
        return virtualInventoryNdcViewRepository.findAllByShipToIdAndPackageQuantityGreaterThan(shipToId,0).stream().map(virtualInventoryNdcViewMapper :: toDto).collect(Collectors.toList());
    }

    // temporary method for Replenish
    @Override
    public List<ReplenishmentVirtualInv> getContractsAndHeaders(UUID shipToId) {
        List<ReplenishmentVirtualInv> contractHeadersList = new ArrayList<>();
        List<ContractsVirtualInventoryDTO> contractsVirtualInventories = getContractsByShipToId(shipToId);
        List<VirtualInventoryNdcUiViewDTO> virtualInventoryHeaderDTOS = getAllByShipToIdAndPackageQuantityGreaterThanZero(shipToId);
        for(VirtualInventoryNdcUiViewDTO virtualInventoryHeaderDTO: virtualInventoryHeaderDTOS){
            ReplenishmentVirtualInv replenishmentVirtualInv = new ReplenishmentVirtualInv();
            replenishmentVirtualInv.setBillToTypeCode(virtualInventoryHeaderDTO.getBillToTypeCode());
            replenishmentVirtualInv.setHeaderId(virtualInventoryHeaderDTO.getVirtualInventoryHeaderId());
            replenishmentVirtualInv.setShipToTypeCode(virtualInventoryHeaderDTO.getShipToTypeCode());
            replenishmentVirtualInv.setBillToId(virtualInventoryHeaderDTO.getBillToId());
            replenishmentVirtualInv.setShipToId(virtualInventoryHeaderDTO.getShipToId());
            replenishmentVirtualInv.setBillToName(virtualInventoryHeaderDTO.getBillToName());
            replenishmentVirtualInv.setShipToName(virtualInventoryHeaderDTO.getShipToName());
            replenishmentVirtualInv.setPackageQuantity(virtualInventoryHeaderDTO.getPackageQuantity());
            replenishmentVirtualInv.setPackageSize(virtualInventoryHeaderDTO.getPackageSize());
            replenishmentVirtualInv.setNdc(virtualInventoryHeaderDTO.getNdc());
            replenishmentVirtualInv.setDrugDescription(virtualInventoryHeaderDTO.getDrugDescription());
            for(ContractsVirtualInventoryDTO contractsVirtualInventory:contractsVirtualInventories){
                if(virtualInventoryHeaderDTO.getShipToId().equals(contractsVirtualInventory.getShipToId())){
                    replenishmentVirtualInv.setContractId(contractsVirtualInventory.getContractId());
                    break;
                }
            }
            contractHeadersList.add(replenishmentVirtualInv);
        }
        return contractHeadersList;
    }

    @Override
    public void persistTrueUpStatus(BillingDTO billingDTO) {
        Long batch = billingDTO.getBatchId();
        List<UUID> contracts = billingDTO.getContractIds();
        List<VirtualInventoryTrueUpStatus> trueUpStatusEntityList = new  ArrayList<>();
        if (null != contracts && !contracts.isEmpty()) {
            try {
                for (UUID contractId: contracts) {
                    if (null != contractId) {

                            VirtualInventoryTrueUpStatus trueUpStatusEntity = new VirtualInventoryTrueUpStatus();
                            trueUpStatusEntity.setBatchId(batch);
                            trueUpStatusEntity.setContractId(contractId);


                            trueUpStatusEntityList.add(trueUpStatusEntity);

                    }
                }
                virtualInventoryTrueUpStatusRepository.save(trueUpStatusEntityList);
                logger.info("Add/Update of TrueUp status was successful");
            } catch (Exception e) {
                logger.error(MessageConstants.TRUE_UP + ": TrueUp Details persistance failed . Stack trace : {} " ,e);
            }

        }


    }
}
