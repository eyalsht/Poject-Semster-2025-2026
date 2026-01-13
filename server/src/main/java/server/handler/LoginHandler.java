package server.handler;

import common.dto.AuthResponse;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import server.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;

public class LoginHandler implements RequestHandler {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_TIME_SEC = 30;

    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<String> creds = (ArrayList<String>) request.getMessage();
            String username = creds.get(0);
            String password = creds.get(1);

            Optional<User> optionalUser = userRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                return new Message(ActionType.LOGIN_RESPONSE,
                    AuthResponse.failure("User does not exist."));
            }

            User user = optionalUser.get();

            synchronized (user) {
                if (user.isBlocked()) {
                    return new Message(ActionType.LOGIN_RESPONSE,
                        AuthResponse.failure("Account is temporarily blocked."));
                }

                if (user.getPassword().equals(password)) {
                    // Successful login
                    user.resetFailedAttempts();
                    userRepository.updateSecurityState(user);
                    return new Message(ActionType.LOGIN_RESPONSE,
                        AuthResponse.success(user));
                } else {
                    return handleFailedLogin(user);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.LOGIN_RESPONSE,
                AuthResponse.failure("Server error during login."));
        }
    }

    private Message handleFailedLogin(User user) {
        user.incrementFailedAttempts();

        if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
            user.setBlocked(true);
            userRepository.updateSecurityState(user);
            startUnblockTimer(user);
            return new Message(ActionType.LOGIN_RESPONSE,
                AuthResponse.failure("Too many failed attempts. Blocked for " + BLOCK_TIME_SEC + " seconds."));
        } else {
            userRepository.updateSecurityState(user);
            int remaining = MAX_ATTEMPTS - user.getFailedAttempts();
            return new Message(ActionType.LOGIN_RESPONSE,
                AuthResponse.failure("Incorrect password. " + remaining + " attempts remaining."));
        }
    }

    private void startUnblockTimer(User user) {
        new Thread(() -> {
            try {
                Thread.sleep(BLOCK_TIME_SEC * 1000L);
                synchronized (user) {
                    user.setBlocked(false);
                    user.resetFailedAttempts();
                    userRepository.updateSecurityState(user);
                    System.out.println("User " + user.getUsername() + " unblocked automatically.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "UnblockTimer-" + user.getUsername()).start();
    }
}
