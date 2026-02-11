package server;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class NotificationService
{
    public static final String ACCOUNT_SID = "SECRET ACCOUNT SID";
    public static final String AUTH_TOKEN = "SECRET ACCOUNT TOKEN";
    public static final String TWILIO_NUMBER = "SECRET TWILIO NUMBER";

    public static void sendEmail(String recipientEmail, String subject, String messageText) {
        // המימוש של JavaMail שדיברנו עליו קודם
        System.out.println("Sending Email to " + recipientEmail + ": " + messageText);
    }

    public static void sendSMS(String phoneNumber, String messageText) {
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            phoneNumber = formatPhoneNumberToInternational(phoneNumber);
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(TWILIO_NUMBER),
                    messageText
            ).create();

            System.out.println("Message SID: " + message.getSid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendSubscriptionAlert(String email, String phone, String firstName, int days, String city) {
        String subject = "GCM Subscription Expiry Warning";
        String message = "Hello " + firstName + ",\n\n" +
                "This is a reminder that your GCM city subscription to " + city + " will expire in " + days + " days.\n" +
                "To ensure uninterrupted access to your maps, please renew your subscription soon.\n\n" +
                "Best regards,\nGCM Team";

        sendEmail(email, subject, message);
        if (phone != null && !phone.isEmpty()) {
            sendSMS(phone, "GCM Reminder: Hello "+firstName+"! Your subscription to " + city + " expires in " + days + " days!");
        }
    }

    public static void sendRegistrationAlert(String email, String phone, String firstName) {
        String subject = "GCM Registration";
        String message = "Hello " + firstName + ",\n\n" +
                "Welcome to GCM Services!\n" +
                "We're so excited you joined us.";

        sendEmail(email, subject, message);
        if (phone != null && !phone.isEmpty()) {
            sendSMS(phone, "Welcome to GCM, " + firstName + "! Check out our app.");
        }
    }
    public static String formatPhoneNumberToInternational(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            cleaned = "972" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }
}