package server.controller;

import common.content.City;
import common.content.GCMMap;
import common.purchase.PurchasedMapSnapshot;
import common.purchase.Subscription;
import common.user.Client;
import common.user.User;
import server.repository.*;
import server.service.PaymentService;

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
     * @return null if purchase was successful, or an error reason string
     */
    public String processPurchase(int userId, Integer cityId, Integer mapId, String purchaseType,
                                   String creditCardToken, int monthsToAdd) {

        System.out.println("Processing " + purchaseType + " purchase for UserID: " + userId +
            ", CityID: " + cityId + ", MapID: " + mapId);

        try {
            // Step 1: Load user from database
            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalUser.isEmpty()) {
                return "User not found with ID: " + userId;
            }

            User user = optionalUser.get();

            // Step 2: Validate payment
            if (user instanceof Client) {
                common.purchase.PaymentDetails pd = userRepository.loadPaymentDetails(userId);
                System.out.println("[DEBUG] PaymentDetails for userId=" + userId + ": " + pd);
                if (pd != null) {
                    System.out.println("[DEBUG] card=" + pd.getCreditCardNumber()
                        + " expiry=" + pd.getExpiryMonth() + "/" + pd.getExpiryYear());
                }
                if (!paymentService.validate(pd)) {
                    return "Payment validation failed: " + (pd == null ? "No payment details on file" :
                        "Card ending " + (pd.getCreditCardNumber() != null ? pd.getCreditCardNumber().substring(Math.max(0, pd.getCreditCardNumber().length()-4)) : "????") +
                        " exp " + pd.getExpiryMonth() + "/" + pd.getExpiryYear());
                }
            } else {
                if (!paymentService.validate(creditCardToken)) {
                    return "Payment validation failed for token";
                }
            }

            // Step 3: Handle based on purchase type
            if (PURCHASE_TYPE_SUBSCRIPTION.equals(purchaseType)) {
                // Subscriptions require a city
                if (cityId == null) {
                    return "City ID is required for SUBSCRIPTION purchase";
                }

                Optional<City> optionalCity = cityRepository.findById(cityId);
                if (optionalCity.isEmpty()) {
                    return "City not found with ID: " + cityId;
                }

                City city = optionalCity.get();
                double monthlyPrice = city.getPriceSub();

                if (monthlyPrice <= 0) {
                    return "Invalid subscription price for city: " + city.getName() + " (price=" + monthlyPrice + ")";
                }

                return processSubscriptionPurchase(user, city, monthlyPrice, monthsToAdd);

            } else if (PURCHASE_TYPE_ONE_TIME.equals(purchaseType)) {
                // One-time purchases are for maps only - no city required
                if (mapId == null) {
                    return "Map ID is required for ONE_TIME purchase";
                }

                Optional<GCMMap> optionalMap = mapRepository.findById(mapId);
                if (optionalMap.isEmpty()) {
                    return "Map not found with ID: " + mapId;
                }

                GCMMap map = optionalMap.get();
                double price = map.getPrice();

                if (price <= 0) {
                    return "Invalid price for map: " + map.getName() + " (price=" + price + ")";
                }

                return processOneTimePurchase(user, map, price);

            } else {
                return "Invalid purchase type: " + purchaseType;
            }

        } catch (Exception e) {
            System.err.println("Purchase failed with exception: " + e.getMessage());
            e.printStackTrace();
            return "Server exception: " + e.getMessage();
        }
    }

    /**
     * Process a subscription purchase with renewal and duration discounts.
     * @return null on success, error reason on failure
     */
    private String processSubscriptionPurchase(User user, City city, double monthlyPrice, int monthsToAdd) {
        try {
            // Check for existing active subscription (renewal detection)
            Subscription existing = purchaseRepository.findLatestSubscription(user.getId(), city.getId());
            boolean isRenewal = (existing != null && existing.getExpirationDate() != null
                    && !existing.getExpirationDate().isBefore(LocalDate.now()));

            // Calculate total price with discounts
            double totalPrice = monthlyPrice * monthsToAdd;

            // Duration discounts: 3mo=5%, 6mo=10%, 12mo=15%
            double durationDiscount = 0;
            if (monthsToAdd >= 12) {
                durationDiscount = 0.15;
            } else if (monthsToAdd >= 6) {
                durationDiscount = 0.10;
            } else if (monthsToAdd >= 3) {
                durationDiscount = 0.05;
            }

            // Renewal discount: 10%
            double renewalDiscount = isRenewal ? 0.10 : 0;

            // Combine discounts (additive)
            double totalDiscount = durationDiscount + renewalDiscount;
            totalPrice = totalPrice * (1 - totalDiscount);

            // Create subscription record
            Subscription subscription = purchaseRepository.createSubscription(user, city, totalPrice, monthsToAdd);

            System.out.println("Subscription purchase completed for UserID: " + user.getId() +
                ", City: " + city.getName() +
                ", Months: " + monthsToAdd +
                ", Renewal: " + isRenewal +
                ", Total discount: " + (totalDiscount * 100) + "%" +
                ", Price: $" + String.format("%.2f", totalPrice) +
                ", Expires: " + subscription.getExpirationDate());
            return null; // success

        } catch (Exception e) {
            System.err.println("Failed to process subscription purchase: " + e.getMessage());
            e.printStackTrace();
            return "Subscription creation failed: " + e.getMessage();
        }
    }

    /**
     * Process a one-time map purchase with upgrade discount.
     * @return null on success, error reason on failure
     */
    private String processOneTimePurchase(User user, GCMMap map, double price) {
        try {
            // Check if user has a previous version (upgrade detection)
            PurchasedMapSnapshot existingSnapshot = purchaseRepository.findPurchasedSnapshot(user.getId(), map.getId());

            if (existingSnapshot != null) {
                String existingVersion = existingSnapshot.getPurchasedVersion();
                String currentVersion = map.getVersion();

                if (currentVersion.equals(existingVersion)) {
                    return "You already own this map version (v" + currentVersion + ")";
                }

                // Apply 50% upgrade discount
                price = price * 0.50;
                System.out.println("Upgrade detected: applying 50% discount for map " + map.getName());
            }

            // Create one-time purchase record (also creates snapshot for the user)
            purchaseRepository.createOneTimePurchase(user, map, price);

            System.out.println("One-time purchase completed for UserID: " + user.getId() +
                ", MapID: " + map.getId() + ", Version: " + map.getVersion() +
                ", Price: $" + String.format("%.2f", price));
            return null; // success

        } catch (Exception e) {
            System.err.println("Failed to process one-time purchase: " + e.getMessage());
            e.printStackTrace();
            return "Map purchase failed: " + e.getMessage();
        }
    }
}
