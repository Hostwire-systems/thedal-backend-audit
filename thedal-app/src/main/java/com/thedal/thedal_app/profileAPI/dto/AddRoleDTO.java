package com.thedal.thedal_app.profileAPI.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddRoleDTO {

	@NotBlank(message = "40102")
    private String roleName;

	@NotNull(message = "40103")
    private List<String> permission;
	@NotNull(message = "40105")
	private Map<String, List<String>> rolePermission;

    @NotBlank(message = "40104")
    private String description;
	
}
