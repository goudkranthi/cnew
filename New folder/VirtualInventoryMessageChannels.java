package com.capturerx.cumulus4.virtualinventory.messagechannels;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface VirtualInventoryMessageChannels {

    String VIRTUAL_INV_ORCHESTRATOR = "vIOrchestratorRequestChannel";
    String ORCHESTRATOR_RESPONSE = "vIOrchestratorResponsechannel";
    String ADJUDICATION_SUMMARY_PRODUCER = "summaryProducer";
    String DATAINGESTION_CHANNEL = "dataIngestionChannel";
    String DATAEXPORTS_CHANNEL = "dataExportsChannel";
    public static final String ADJUDICATION_SUMMARY_LISTENER = "summaryInboxConsumer";
    public static final String TRUE_UP_WORKER_LISTENER = "trueUpWorkerListerner";



    @Input(VirtualInventoryMessageChannels.ADJUDICATION_SUMMARY_LISTENER)
    MessageChannel summaryInboxConsumer();

    @Output(VirtualInventoryMessageChannels.ADJUDICATION_SUMMARY_PRODUCER)
    MessageChannel summaryProducer();

    @Input(VirtualInventoryMessageChannels.VIRTUAL_INV_ORCHESTRATOR)
    MessageChannel vIOrchestratorRequestChannel();

    @Output(VirtualInventoryMessageChannels.ORCHESTRATOR_RESPONSE)
    MessageChannel vIOrchestratorResponseChannel();

    @Input(VirtualInventoryMessageChannels.DATAINGESTION_CHANNEL)
    MessageChannel dataIngestionChannel();

    @Input(VirtualInventoryMessageChannels.DATAEXPORTS_CHANNEL)
    MessageChannel dataExportsChannel();

    @Input(VirtualInventoryMessageChannels.TRUE_UP_WORKER_LISTENER)
    MessageChannel trueUpWorkerListener();


}
