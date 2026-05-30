package com.thedal.thedal_app.voter;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDetail {
	@JsonProperty("epicNumber")
	@Field("epicNumber")
    private String epicNumber;
	
	@JsonProperty("name")
	@Field("name")
    private String name;

}
