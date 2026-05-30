package com.thedal.thedal_app.cpanel;

public class VoterHistoryReorderRequest {
        private Long voterHistoryId;
        private Integer newOrderIndex;
        
        public Long getVoterHistoryId() {
            return voterHistoryId;
        }
        public void setVoterHistoryId(Long voterHistoryId) {
            this.voterHistoryId = voterHistoryId;
        }
        public Integer getNewOrderIndex() {
            return newOrderIndex;
        }
        public void setNewOrderIndex(Integer newOrderIndex) {
            this.newOrderIndex = newOrderIndex;
        }
}
