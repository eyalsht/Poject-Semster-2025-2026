package server.service;

import common.purchase.PaymentDetails;
import java.time.YearMonth;

public class PaymentService {

    public boolean validate(String creditCardToken) {
        System.out.println("External Payment System: Validating card " + creditCardToken + " -> Approved");
        return true;
    }

    /**
     * Validates payment details by checking card expiry date.
     * Returns false if card is expired or details are null/malformed.
     */
    public boolean validate(PaymentDetails pd) {
        if (pd == null) {
            System.err.println("Payment validation failed: PaymentDetails is null");
            return false;
        }

        String expiryMonth = pd.getExpiryMonth();
        String expiryYear = pd.getExpiryYear();

        if (expiryMonth == null || expiryYear == null || expiryMonth.isBlank() || expiryYear.isBlank()) {
            System.err.println("Payment validation failed: Missing expiry date");
            return false;
        }

        try {
            int month = Integer.parseInt(expiryMonth);
            int year = Integer.parseInt(expiryYear);

            // Handle 2-digit year
            if (year < 100) {
                year += 2000;
            }

            YearMonth cardExpiry = YearMonth.of(year, month);
            YearMonth now = YearMonth.now();

            if (cardExpiry.isBefore(now)) {
                System.err.println("Payment validation failed: Card expired (" + cardExpiry + ")");
                return false;
            }

            System.out.println("External Payment System: Card validated (expires " + cardExpiry + ") -> Approved");
            return true;

        } catch (NumberFormatException e) {
            System.err.println("Payment validation failed: Invalid expiry format (" + expiryMonth + "/" + expiryYear + ")");
            return false;
        } catch (Exception e) {
            System.err.println("Payment validation failed: " + e.getMessage());
            return false;
        }
    }
}
