package server;

import java.time.LocalDate;

public class PurchaseController {

    // Purchase type constants
    public static final String PURCHASE_TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String PURCHASE_TYPE_ONE_TIME = "ONE_TIME";

    // Singleton instance
    private static PurchaseController instance;

    // Dependencies
    private final PaymentService paymentService;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the PaymentService dependency.
     */
    private PurchaseController() {
        this.paymentService = new PaymentService();
    }

    /**
     * Returns the singleton instance of PurchaseController.
     * Creates the instance if it doesn't exist.
     */
    public static synchronized PurchaseController getInstance() {
        if (instance == null) {
            instance = new PurchaseController();
        }
        return instance;
    }


    public boolean processPurchase(int userId, int cityId, Integer mapId, String purchaseType, 
                                   String creditCardToken, int monthsToAdd) {
        System.out.println("Processing " + purchaseType + " purchase for UserID: " + userId + 
                         ", CityID: " + cityId + ", MapID: " + mapId + 
                         ", MonthsToAdd: " + monthsToAdd + " with token: " + creditCardToken);

        // Step 1: Fetch Base Price and Calculate Expected Price
        double basePrice;
        double expectedPrice;
        
        if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
            // Security check: Validate monthsToAdd is 1, 3, or 6
            if (monthsToAdd != 1 && monthsToAdd != 3 && monthsToAdd != 6) {
                System.err.println("Purchase failed: Invalid monthsToAdd value: " + monthsToAdd + 
                                 ". Must be 1, 3, or 6 for SUBSCRIPTION purchases.");
                return false;
            }
            
            // Fetch base monthly price
            basePrice = DBController.getCitySubscriptionPrice(cityId);
            
            // Validate base price
            if (basePrice <= 0) {
                System.err.println("Purchase failed: Base subscription price not found or invalid for CityID: " + cityId);
                return false;
            }
            
            // Calculate total price based on months
            expectedPrice = basePrice * monthsToAdd;
            System.out.println("Base monthly price: " + basePrice + ", Months: " + monthsToAdd + 
                             ", Total price: " + expectedPrice);
            
        } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
            // One-time purchase: fetch city one-time price (multiplier is effectively 1, ignore months)
            expectedPrice = DBController.getCityOneTimePrice(cityId);
            
            // Validate price
            if (expectedPrice <= 0) {
                System.err.println("Purchase failed: One-time price not found or invalid for CityID: " + cityId);
                return false;
            }
            
            System.out.println("One-time price determined: " + expectedPrice);
            
        } else {
            System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
            return false;
        }

        System.out.println("Expected price: " + expectedPrice + " for " + purchaseType + " purchase, UserID: " + userId);

        // Step 2: Validate payment with expected price
        boolean paymentValid = paymentService.validate(creditCardToken, expectedPrice);
        
        if (!paymentValid) {
            System.err.println("Purchase failed: Payment validation failed for UserID: " + userId + 
                             ", Amount: " + expectedPrice);
            return false;
        }

        System.out.println("Payment validated successfully for UserID: " + userId + ", Amount: " + expectedPrice);

        // Step 3: Log the purchase with total price paid
        boolean logSuccess = DBController.logPurchase(userId, cityId, mapId, purchaseType, expectedPrice);
        
        if (!logSuccess) {
            System.err.println("Purchase failed: Failed to log purchase in database for UserID: " + userId);
            return false;
        }

        System.out.println("Purchase logged successfully for UserID: " + userId + ", Amount: " + expectedPrice);

        // Step 4: Conditional logic based on purchase type
        if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
            // Update subscription expiry date by adding the correct number of months
            LocalDate newExpiryDate = LocalDate.now().plusMonths(monthsToAdd);
            System.out.println("Calculated new subscription expiry date: " + newExpiryDate + 
                             " (added " + monthsToAdd + " months) for UserID: " + userId);
            
            boolean subscriptionSuccess = DBController.updateUserSubscription(userId, newExpiryDate);
            
            if (!subscriptionSuccess) {
                System.err.println("Purchase failed: Failed to update subscription for UserID: " + userId);
                return false;
            }
            
            System.out.println("Subscription updated successfully for UserID: " + userId + 
                             ". Expires: " + newExpiryDate);
            
        } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
            // One-time purchase: just log it, no subscription update needed
            System.out.println("One-time purchase completed for UserID: " + userId + ", CityID: " + cityId);
            
        } else {
            System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
            return false;
        }

        System.out.println("Purchase completed successfully for UserID: " + userId + 
                         ", Type: " + purchaseType + ", Total Amount: " + expectedPrice);
        return true;
    }
}

