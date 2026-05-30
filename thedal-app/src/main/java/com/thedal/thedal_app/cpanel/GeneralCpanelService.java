package com.thedal.thedal_app.cpanel;


import java.util.List;

import com.thedal.thedal_app.cpanel.dtos.GeneralCpanelRequest;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageDTO;
import com.thedal.thedal_app.settings.electionsettings.dto.LanguageResponseDTO;

public interface GeneralCpanelService {
    ThedalResponse<Void> saveTnC(GeneralCpanelRequest request);
	ThedalResponse<String> getTnC();
	ThedalResponse<String> getPrivacyPolicy();
	ThedalResponse<String> getFaq();
	ThedalResponse<String> getAbout();
	
}
