package com.thedal.thedal_app.election.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterFieldOrderRequestDTO {
    private List<FieldOrderItem> fields;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOrderItem {
        private String name;
        private Integer orderIndex;
        
        // Support both field names for backwards compatibility
        @com.fasterxml.jackson.annotation.JsonAlias("newOrderIndex")
        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }
        
        // Defensive getter that handles null values
        public Integer getOrderIndex() {
            return orderIndex != null ? orderIndex : 0;
        }
    }
}
