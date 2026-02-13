package DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SetPreferredLanguageResponse {
	 private String responseCode;
	  @JsonProperty("Message")
	 private String Message;
	 
	 public String getResponseCode() {
		 return responseCode;
	 }
	 public void setResponseCode(String responseCode) {
		 this.responseCode = responseCode;
	 }
	 public String getMessage() {
		 return Message;
	 }
	 public void setMessage(String message) {
		 Message = message;
	 }
	 
	 
	 @Override
	    public String toString() {
	        return "SetPreferredLanguageResponse{" +
	                "responseCode='" + responseCode + '\'' +
	                ", Message='" + Message + '\'' +
	                '}';
	    }
}
