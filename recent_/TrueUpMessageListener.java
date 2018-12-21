package com.capturerx.cumulus.virtualinventory.trueupgenerator.MessageListener;

import com.capturerx.cumulus4.virtualinventory.messagechannels.VirtualInventoryMessageChannels;
import com.capturerx.cumulus4.virtualinventory.messages.SampleMessage;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@EnableBinding(VirtualInventoryMessageChannels.class)
@Service
@Slf4j
public class TrueUpMessageListener {

    @StreamListener(VirtualInventoryMessageChannels.TRUE_UP_WORKER_LISTENER)
    public void processMessage(SampleMessage message){

        System.out.print("Consumed trueUp Message"+ message.getName());


    }


}
