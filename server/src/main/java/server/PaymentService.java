package server;

public class PaymentService {

    public boolean validate(String creditCardToken) {
        System.out.println("External Payment System: Validating card " + creditCardToken + " -> Approved");
        return true;
    }
}

