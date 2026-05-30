package com.thedal.thedal_app.voter;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Embeddable
@Data
@Setter
@Getter
@AllArgsConstructor
public class Address {
	
	public Address() {}

	@NotBlank(message = "Street is mandatory")
	@Column(name = "street")
    @JsonProperty("street")
    private String street;

	@NotBlank(message = "City is mandatory")
    @Column(name = "city")
    @JsonProperty("city")
    private String city;

	@NotBlank(message = "State is mandatory")
    @Column(name = "state")
    @JsonProperty("state")
    private String state;

	@NotBlank(message = "Postal code is mandatory")
    @Size(min = 5, max = 6, message = "Postal code must be between 5 and 6 characters")
    @Column(name = "postal_code")
    @JsonProperty("postal_code")
    private String postalCode;

	@NotBlank(message = "Country is mandatory")
    @Column(name = "country")
    @JsonProperty("country")
    private String country;

}
