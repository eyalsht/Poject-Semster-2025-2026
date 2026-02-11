package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import server.repository.PurchaseRepository;

import java.util.ArrayList;
import java.util.List;

public class GetUserPurchasedMapsHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            int userId = (Integer) request.getMessage();
            PurchaseRepository repo = PurchaseRepository.getInstance();

            List<PurchasedMapSnapshot> snapshots = repo.findPurchasedMapsByUserId(userId);

            return new Message(ActionType.GET_USER_PURCHASED_MAPS_RESPONSE, new ArrayList<>(snapshots));

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.GET_USER_PURCHASED_MAPS_RESPONSE, new ArrayList<>());
        }
    }
}
