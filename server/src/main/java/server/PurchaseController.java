package server;

import common.content.City;
import common.content.GCMMap;
import common.purchase.Subscription;
import common.user.Client;
import common.user.User;
import server.repository.*;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Controller for handling purchase operations.
 * Refactored to use Repository pattern instead of DBController.
 */
public class PurchaseController {

    // Purchase type constants
    public static final String PURCHASE_TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    public static final String PURCHASE_TYPE_ONE_TIME = "ONE_TIME";

    // Singleton instance
    private static PurchaseController instance;

    // Dependencies
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final MapRepository mapRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentService paymentService;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private PurchaseController() {
        this.userRepository = UserRepository.getInstance();
        this.cityRepository = CityRepository.getInstance();
        this.mapRepository = MapRepository.getInstance();
        this.purchaseRepository = PurchaseRepository.getInstance();
        this.paymentService = new PaymentService();
    }

    /**
     * Returns the singleton instance of PurchaseController.
     */
    public static synchronized PurchaseController getInstance() {
        if (instance == null) {
            instance = new PurchaseController();
        }
        return instance;
    }

    /**
     * Process a purchase request.
     * 
     * @param userId          The ID of the user making the purchase
     * @param cityId          The ID of the city
     * @param mapId           The ID of the map (null for subscription)
     * @param purchaseType    SUBSCRIPTION or ONE_TIME
     * @param creditCardToken Payment token
     * @param monthsToAdd     Months to add for subscription
     * @return true if purchase was successful
     */
    public boolean processPurchase(int userId, int cityId, Integer mapId, String purchaseType,
                                   String creditCardToken, int monthsToAdd) {
        
        System.out.println("Processing " + purchaseType + " purchase for UserID: " + userId +
            ", CityID: " + cityId + ", MapID: " + mapId);

        try {
            // Step 1: Load entities from database
            Optional<User> optionalUser = userRepository.findById(userId);
            Optional<City> optionalCity = cityRepository.findById(cityId);

            if (optionalUser.isEmpty()) {
                System.err.println("Purchase failed: User not found with ID: " + userId);
                return false;
            }

            if (optionalCity.isEmpty()) {
                System.err.println("Purchase failed: City not found with ID: " + cityId);
                return false;
            }

            User user = optionalUser.get();
            City city = optionalCity.get();
            GCMMap map = null;

            // Step 2: Determine price based on purchase type
            double price;

            if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
                price = city.getPriceSub();
                
                if (price <= 0) {
                    System.err.println("Purchase failed: Invalid subscription price for city: " + city.getName());
                    return false;
                }

            } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
                if (mapId == null) {
                    System.err.println("Purchase failed: Map ID is required for ONE_TIME purchase");
                    return false;
                }

                Optional<GCMMap> optionalMap = mapRepository.findById(mapId);
                if (optionalMap.isEmpty()) {
                    System.err.println("Purchase failed: Map not found with ID: " + mapId);
                    return false;
                }

                map = optionalMap.get();
                price = map.getPrice();

                if (price <= 0) {
                    System.err.println("Purchase failed: Invalid price for map: " + map.getName());
                    return false;
                }

            } else {
                System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
                return false;
            }

            System.out.println("Price determined: " + price + " for " + purchaseType);

            // Step 3: Validate payment
            boolean paymentValid = paymentService.validate(creditCardToken);

            if (!paymentValid) {
                System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
                return false;
            }

            System.out.println("Payment validated successfully for UserID: " + userId + ", Amount: " + price);

            // Step 4: Create the purchase record and update user if needed
            if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
                return processSubscriptionPurchase(user, city, price, monthsToAdd);
            } else {
                return processOneTimePurchase(user, city, map, price);
            }

        } catch (Exception e) {
            System.err.println("Purchase failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Process a subscription purchase.
     */
    private boolean processSubscriptionPurchase(User user, City city, double price, int monthsToAdd) {
        try {
            // Create subscription record
            Subscription subscription = purchaseRepository.createSubscription(user, city, price, monthsToAdd);

            // Update user's subscription expiry (if user is a Client)
            if (user instanceof Client client) {
                LocalDate newExpiry = subscription.getExpirationDate();
                userRepository.updateSubscriptionExpiry(user.getId(), newExpiry);
                System.out.println("Subscription updated for UserID: " + user.getId() + 
                    ". Expires: " + newExpiry);
            }

            System.out.println("Subscription purchase completed successfully for UserID: " + user.getId());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to process subscription purchase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Process a one-time map purchase.
     */
    private boolean processOneTimePurchase(User user, City city, GCMMap map, double price) {
        try {
            // Create one-time purchase record
            purchaseRepository.createOneTimePurchase(user, city, map, price);

            System.out.println("One-time purchase completed for UserID: " + user.getId() + 
                ", MapID: " + map.getId());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to process one-time purchase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

