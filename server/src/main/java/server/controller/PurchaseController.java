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

            // Step 2: Validate payment using card details if user is a Client
            if (user instanceof Client) {
                // Load Client specifically to ensure @Embedded PaymentDetails is
                // fully initialised (JOINED inheritance + detached session can
                // leave the embedded fields null when loaded via User.class).
                Optional<Client> optClient = userRepository.findClientById(userId);
                if (optClient.isEmpty()) {
                    System.err.println("Purchase failed: Client not found with ID: " + userId);
                    return false;
                }
                Client client = optClient.get();
                if (!paymentService.validate(client.getPaymentDetails())) {
                    System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
                    return false;
                }
            } else {
                // Fallback to token validation for non-clients
                if (!paymentService.validate(creditCardToken)) {
                    System.err.println("Purchase failed: Payment validation failed for UserID: " + userId);
                    return false;
                }
            }

            // Step 3: Handle based on purchase type
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
                double monthlyPrice = city.getPriceSub();

                if (monthlyPrice <= 0) {
                    System.err.println("Purchase failed: Invalid subscription price for city: " + city.getName());
                    return false;
                }

                return processSubscriptionPurchase(user, city, monthlyPrice, monthsToAdd);

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
                double price = map.getPrice();

                if (price <= 0) {
                    System.err.println("Purchase failed: Invalid price for map: " + map.getName());
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
     * Process a subscription purchase with renewal and duration discounts.
     */
    private boolean processSubscriptionPurchase(User user, City city, double monthlyPrice, int monthsToAdd) {
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
            return true;

        } catch (Exception e) {
            System.err.println("Failed to process subscription purchase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Process a one-time map purchase with upgrade discount.
     */
    private boolean processOneTimePurchase(User user, GCMMap map, double price) {
        try {
            // Check if user has a previous version (upgrade detection)
            PurchasedMapSnapshot existingSnapshot = purchaseRepository.findPurchasedSnapshot(user.getId(), map.getId());

            if (existingSnapshot != null) {
                String existingVersion = existingSnapshot.getPurchasedVersion();
                String currentVersion = map.getVersion();

                if (currentVersion.equals(existingVersion)) {
                    System.err.println("Purchase failed: User already owns this map version");
                    return false;
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
            return true;

        } catch (Exception e) {
            System.err.println("Failed to process one-time purchase: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
