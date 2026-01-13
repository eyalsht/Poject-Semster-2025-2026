package server;

public class PaymentService {

    public boolean validate(String creditCardToken, double expectedPrice) {
        System.out.println("External Payment System: Validating card " + creditCardToken + 
                         " for amount $" + expectedPrice + " -> Approved");
        return true;
    }
}

