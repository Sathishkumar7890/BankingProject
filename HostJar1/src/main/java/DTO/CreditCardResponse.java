package DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreditCardResponse {

    private String responseCode;

    // Map JSON "Message" correctly
    @JsonProperty("Message")
    private String message;

    private List<CustomerCardDetails> creditCards;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public List<CustomerCardDetails> getCreditCards() { return creditCards; }
    public void setCreditCards(List<CustomerCardDetails> creditCards) { this.creditCards = creditCards; }

    @Override
    public String toString() {
        return "CreditCardResponse{" +
                "responseCode='" + responseCode + '\'' +
                ", message='" + message + '\'' +
                ", creditCards=" + creditCards +
                '}';
    }
}
