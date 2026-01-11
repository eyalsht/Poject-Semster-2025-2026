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
                         " with token: " + creditCardToken);

        // Step 1: Determine and validate price
        double price;
        if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
            price = DBController.getCitySubscriptionPrice(cityId);
        }
        else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
            if (mapId == null) {
                System.err.println("Purchase failed: Map ID is required for ONE_TIME purchase");
                return false;
            }
            price = DBController.getMapOneTimePrice(mapId);
        } else {
            System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
            return false;
        }

        // Validate price
        if (price <= 0) {
            System.err.println("Purchase failed: Price not found or invalid for UserID: " + userId + 
                             ", PurchaseType: " + purchaseType);
            return false;
        }

        System.out.println("Price determined: " + price + " for " + purchaseType + " purchase, UserID: " + userId);

        // Step 2: Validate payment
        boolean paymentValid = paymentService.validate(creditCardToken);
        
        if (!paymentValid) {
            System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
            return false;
        }

        System.out.println("Payment validated successfully for UserID: " + userId + ", Amount: " + price);

        // Step 3: Log the purchase
        boolean logSuccess = DBController.logPurchase(userId, cityId, mapId, purchaseType, price);
        
        if (!logSuccess) {
            System.err.println("Purchase failed: Failed to log purchase in database for UserID: " + userId);
            return false;
        }

        System.out.println("Purchase logged successfully for UserID: " + userId);

        // Step 4: Conditional logic based on purchase type
        if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
            // Update subscription expiry date
            LocalDate newExpiryDate = LocalDate.now().plusMonths(monthsToAdd);
            System.out.println("Calculated new subscription expiry date: " + newExpiryDate + " for UserID: " + userId);
            
            boolean subscriptionSuccess = DBController.updateUserSubscription(userId, newExpiryDate);
            
            if (!subscriptionSuccess) {
                System.err.println("Purchase failed: Failed to update subscription for UserID: " + userId);
                return false;
            }
            
            System.out.println("Subscription updated successfully for UserID: " + userId + ". Expires: " + newExpiryDate);
            
        } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
            // One-time purchase: just log it, no subscription update needed
            System.out.println("One-time purchase completed for UserID: " + userId + ", MapID: " + mapId);
            
        } else {
            System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
            return false;
        }

        System.out.println("Purchase completed successfully for UserID: " + userId + ", Type: " + purchaseType);
        return true;
    }
}

