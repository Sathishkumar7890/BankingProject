package DTO;

public class ValidateOTPResponse {
    private String responseCode;

    // Map JSON "Message" correctly
    @com.fasterxml.jackson.annotation.JsonProperty("Message")
    private String message;

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    
    @Override
    public String toString() {
        return "ValidateOTPResponse{" +
                "responseCode='" + responseCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
