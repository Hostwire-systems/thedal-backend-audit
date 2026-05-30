package com.thedal.thedal_app.voter.dto;

public class VoterHistoryDTO {
    private Long id;
    private String name;
    private String image;
    private Integer orderIndex;
    
    public VoterHistoryDTO() {}
    
    public VoterHistoryDTO(Long id, String name, String image, Integer orderIndex) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.orderIndex = orderIndex;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
