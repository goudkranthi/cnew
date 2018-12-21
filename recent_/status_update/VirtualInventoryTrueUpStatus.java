package com.capturerx.cumulus4.virtualinventory.models;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "true_up_status")
@Data
public class VirtualInventoryTrueUpStatus extends BaseEntity{

    @Id
    @GeneratedValue(generator = "inquisitive-uuid")
    @GenericGenerator(name = "inquisitive-uuid", strategy = "com.capturerx.cumulus4.virtualinventory.utilities.InquisitiveUUIDGenerator")
    @Column(name = "id")
    private UUID id;
    @Column(name = "contract_id")
    private UUID contractId;
    @Column(name = "batch_id")
    private long batchId;
    @Column(name = "status")
    private String status;
    @Column(name = "description")
    private String description;

}
