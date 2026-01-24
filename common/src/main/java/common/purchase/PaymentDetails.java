package common.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable // For embedding within Client entity
public class PaymentDetails implements Serializable {

    @Column(name = "credit_card_number")
    private String creditCardNumber;

    @Column(name = "cvv")
    private String cvv;

    @Column(name = "expiry_month")
    private String expiryMonth;

    @Column(name = "expiry_year")
    private String expiryYear;

    // Hibernate expects a no-arg constructor
    public PaymentDetails() {}

    public PaymentDetails(String creditCardNumber, String cvv, String expiryMonth, String expiryYear) {
        this.creditCardNumber = creditCardNumber;
        this.cvv = cvv;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
    }

    // Getters & Setters
    public String getCreditCardNumber() { return creditCardNumber; }
    public void setCreditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }

    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }

    @Override
    public String toString() {
        return "PaymentDetails{card='****" + (creditCardNumber != null && creditCardNumber.length() > 4 ? creditCardNumber.substring(creditCardNumber.length() - 4) : "") + "'}";
    }
}