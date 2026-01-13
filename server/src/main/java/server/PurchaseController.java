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
     * @param cityId          The ID of the city (required for subscription, optional for one-time)
     * @param mapId           The ID of the map (required for one-time purchase)
     * @param purchaseType    SUBSCRIPTION or ONE_TIME
     * @param creditCardToken Payment token
     * @param monthsToAdd     Months to add for subscription
     * @return true if purchase was successful
     */
    public boolean processPurchase(int userId, Integer cityId, Integer mapId, String purchaseType,
                                   String creditCardToken, int monthsToAdd) {
    
        System.out.println("Processing " + purchaseType + " purchase for UserID: " + userId +
            ", CityID: " + cityId + ", MapID: " + mapId);

        try {
            // Step 1: Load user from database
            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isEmpty()) {
                System.err.println("Purchase failed: User not found with ID: " + userId);
                return false;
            }

            User user = optionalUser.get();

            // Step 2: Handle based on purchase type
            double price;

            if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
                // Subscriptions require a city
                if (cityId == null) {
                    System.err.println("Purchase failed: City ID is required for SUBSCRIPTION purchase");
                    return false;
                }

                Optional<City> optionalCity = cityRepository.findById(cityId);
                if (optionalCity.isEmpty()) {
                    System.err.println("Purchase failed: City not found with ID: " + cityId);
                    return false;
                }

                City city = optionalCity.get();
                price = city.getPriceSub();
                
                if (price <= 0) {
                    System.err.println("Purchase failed: Invalid subscription price for city: " + city.getName());
                    return false;
                }

                // Validate payment
                if (!paymentService.validate(creditCardToken)) {
                    System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
                    return false;
                }

                return processSubscriptionPurchase(user, city, price, monthsToAdd);

            } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
                // One-time purchases are for maps only - no city required
                if (mapId == null) {
                    System.err.println("Purchase failed: Map ID is required for ONE_TIME purchase");
                    return false;
                }

                Optional<GCMMap> optionalMap = mapRepository.findById(mapId);
                if (optionalMap.isEmpty()) {
                    System.err.println("Purchase failed: Map not found with ID: " + mapId);
                    return false;
                }

                GCMMap map = optionalMap.get();
                price = map.getPrice();

                if (price <= 0) {
                    System.err.println("Purchase failed: Invalid price for map: " + map.getName());
                    return false;
                }

                // Validate payment
                if (!paymentService.validate(creditCardToken)) {
                    System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
                    return false;
                }

                return processOneTimePurchase(user, map, price);

            } else {
                System.err.println("Purchase failed: Invalid purchase type: " + purchaseType);
                return false;
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
     * Note: City parameter removed - maps are purchased directly, not through cities.
     */
    private boolean processOneTimePurchase(User user, GCMMap map, double price) {
        try {
            // Create one-time purchase record (also creates snapshot for the user)
            purchaseRepository.createOneTimePurchase(user, map, price);

            System.out.println("One-time purchase completed for UserID: " + user.getId() + 
                ", MapID: " + map.getId() + ", Version: " + map.getVersion());
            return true;

        } catch (Exception e) {
            System.err.println("Failed to process one-time purchase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

