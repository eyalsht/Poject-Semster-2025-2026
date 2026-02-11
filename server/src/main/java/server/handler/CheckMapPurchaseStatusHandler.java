package server.handler;

import common.content.GCMMap;
import common.dto.MapPurchaseStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import server.repository.MapRepository;
import server.repository.PurchaseRepository;

import java.util.ArrayList;
import java.util.Optional;

public class CheckMapPurchaseStatusHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Integer> params = (ArrayList<Integer>) request.getMessage();
            int userId = params.get(0);
            int mapId = params.get(1);

            PurchaseRepository purchaseRepo = PurchaseRepository.getInstance();
            PurchasedMapSnapshot snapshot = purchaseRepo.findPurchasedSnapshot(userId, mapId);

            Optional<GCMMap> optionalMap = MapRepository.getInstance().findById(mapId);
            double fullPrice = optionalMap.map(GCMMap::getPrice).orElse(0.0);
            String currentVersion = optionalMap.map(GCMMap::getVersion).orElse("1.0");

            if (snapshot == null) {
                // Never purchased
                return new Message(ActionType.CHECK_MAP_PURCHASE_STATUS_RESPONSE,
                    new MapPurchaseStatusDTO(false, false, null, fullPrice, fullPrice));
            }

            String purchasedVersion = snapshot.getPurchasedVersion();
            boolean isCurrentVersion = currentVersion.equals(purchasedVersion);

            if (isCurrentVersion) {
                // Current version owned
                return new Message(ActionType.CHECK_MAP_PURCHASE_STATUS_RESPONSE,
                    new MapPurchaseStatusDTO(true, false, purchasedVersion, 0, fullPrice));
            } else {
                // Older version owned - upgrade available at 50% discount
                return new Message(ActionType.CHECK_MAP_PURCHASE_STATUS_RESPONSE,
                    new MapPurchaseStatusDTO(false, true, purchasedVersion, fullPrice * 0.50, fullPrice));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.CHECK_MAP_PURCHASE_STATUS_RESPONSE,
                new MapPurchaseStatusDTO(false, false, null, 0, 0));
        }
    }
}
