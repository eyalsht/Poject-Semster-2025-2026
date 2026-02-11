package server.service;

import common.purchase.PaymentDetails;
import java.time.YearMonth;

public class PaymentService {

    public boolean validate(String creditCardToken) {
        return true;
    }

    /**
     * Validates payment details by checking card expiry date.
     * Returns false if card is expired or details are null/malformed.
     */
    public boolean validate(PaymentDetails pd) {
        if (pd == null) {
            return false;
        }

        String expiryMonth = pd.getExpiryMonth();
        String expiryYear = pd.getExpiryYear();

        if (expiryMonth == null || expiryYear == null || expiryMonth.isBlank() || expiryYear.isBlank()) {
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

            return !cardExpiry.isBefore(now);

        } catch (Exception e) {
            return false;
        }
    }
}
