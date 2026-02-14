package server.handler;

import common.dto.AuthResponse;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PaymentDetails;
import server.repository.UserRepository;

import java.util.ArrayList;

public class RegisterHandler implements RequestHandler {

    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            // Assumption is that client sends a list: indices 0-4 are personal details, index 5 is PaymentDetails object
            ArrayList<Object> data = (ArrayList<Object>) request.getMessage();

            String firstName = (String) data.get(0);
            String lastName = (String) data.get(1);
            String email = (String) data.get(2);
            String phone = (String) data.get(3);
            String username = (String) data.get(4);
            String password = (String) data.get(5);

            PaymentDetails payment = (PaymentDetails) data.get(6);

            // 1. Check if user exists (User Validation)
            if (userRepository.isUsernameTaken(username)) {
                return new Message(ActionType.REGISTER_RESPONSE,
                        AuthResponse.failure("Username already exists. Please choose another."));
            }

            if (userRepository.isEmailTaken(email)) {
                return new Message(ActionType.REGISTER_RESPONSE,
                        AuthResponse.failure("Email already registered."));
            }

            // 2. Validate against external credit system (Mock) — skip if no payment provided
            if (payment != null && !validatePaymentWithExternalSystem(payment)) {
                return new Message(ActionType.REGISTER_RESPONSE,
                        AuthResponse.failure("Credit card authorization failed."));
            }

            // 3. Save client to DB
            boolean success = userRepository.registerClient(
                    firstName, lastName, email, phone, username, password, payment
            );

            if (success) {
                return new Message(ActionType.REGISTER_RESPONSE,
                        AuthResponse.success("Registration successful! You can now log in."));
            } else {
                return new Message(ActionType.REGISTER_RESPONSE,
                        AuthResponse.failure("Database error during registration."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.REGISTER_RESPONSE,
                    AuthResponse.failure("Server error: " + e.getMessage()));
        }
    }

    // Simulation of external credit check
    private boolean validatePaymentWithExternalSystem(PaymentDetails payment) {
        // "external system", just check the valid length
        return payment.getCreditCardNumber().length() == 16 && payment.getCvv().length() == 3;
    }
}