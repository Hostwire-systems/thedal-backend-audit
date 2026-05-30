package com.thedal.thedal_app.files;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
@CompoundIndexes({
    @CompoundIndex(name = "handler_file_order_idx", def = "{'handlerType': 1, 'handlerFileId': 1, 'orderIndex': 1}"),
    @CompoundIndex(name = "handler_file_active_idx", def = "{'handlerType': 1, 'handlerFileId': 1, 'isActive': 1}"),
    @CompoundIndex(name = "handler_file_whatsapp_idx", def = "{'handlerType': 1, 'handlerFileId': 1, 'whatsappForward': 1}")
})
public class FilesMongo {

    @Id
    private Long id;

    @Indexed
    private HandlerType handlerType;

    @Indexed
    private Long handlerFileId;

    private String fileName;
    private String url;
    
    @Indexed
    private Integer orderIndex;
    
    @Indexed
    private Boolean whatsappForward = false;
    
    @Indexed
    private Boolean isActive = true;

    // MongoDB specific fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor from Files entity
    public FilesMongo(Files file) {
        this.id = file.getId();
        this.handlerType = file.getHandlerType();
        this.handlerFileId = file.getHandlerFileId();
        this.fileName = file.getFileName();
        this.url = file.getUrl();
        this.orderIndex = file.getOrderIndex();
        this.whatsappForward = file.getWhatsappForward();
        this.isActive = file.getIsActive();
        
        // Set timestamps
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
