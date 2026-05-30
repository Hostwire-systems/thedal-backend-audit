package com.thedal.thedal_app.sendgrid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sendgrid.helpers.mail.objects.Personalization;

@Component
public class DynamicTemplatePersonalization extends Personalization {

	  @JsonProperty("dynamic_template_data")
	  private Map<String, Object> dynamicTemplateData;
	
	
	  @JsonProperty("dynamic_template_data")
	  public Map<String, Object> getDynamicTemplateData() {
	    return dynamicTemplateData == null
	        ? Collections.<String, Object>emptyMap() : dynamicTemplateData;
	  }
	  
	  public void addDynamicTemplateData(String key, Object value) {
		    if (dynamicTemplateData == null) {
		      dynamicTemplateData = new HashMap<String, Object>();
		    }
		    dynamicTemplateData.put(key, value);
		  }

}
