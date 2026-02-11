package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.controller.PurchaseController;

import java.util.ArrayList;

public class PurchaseHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Object> data = (ArrayList<Object>) request.getMessage();

            int userId = (Integer) data.get(0);
            Integer cityId = data.get(1) != null ? (Integer) data.get(1) : null;
            Integer mapId = (data.get(2) != null) ? (Integer) data.get(2) : null;
            String purchaseType = (String) data.get(3);
            String creditCardToken = (String) data.get(4);
            int monthsToAdd = (Integer) data.get(5);

            boolean success = PurchaseController.getInstance().processPurchase(
                userId, cityId, mapId, purchaseType, creditCardToken, monthsToAdd
            );

            return new Message(ActionType.PURCHASE_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.PURCHASE_RESPONSE, false);
        }
    }
}
