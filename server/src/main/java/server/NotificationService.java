package server;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class NotificationService
{
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String EMAIL_USER = System.getenv("EMAIL_USER");
    private static final String EMAIL_PASSWORD = System.getenv("EMAIL_PASSWORD");

    public static final String TWILIO_ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    public static final String TWILIO_NUMBER = System.getenv("TWILIO_NUMBER");

    public static void sendEmail(String recipientEmail, String subject, String messageText)
    {
        System.out.println("[DEBUG] Attempting to send mail to: " + recipientEmail);
        System.out.println("[DEBUG] EMAIL_USER is: " + EMAIL_USER);
        System.out.println("[DEBUG] EMAIL_PASSWORD length: " + (EMAIL_PASSWORD != null ? EMAIL_PASSWORD.length() : "NULL"));
        Properties prop = new Properties();
        prop.put("mail.smtp.host", SMTP_HOST);
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); // אבטחה TLS
        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_USER, EMAIL_PASSWORD);
            }
        });
        session.setDebug(true);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_USER));
            message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(messageText);

            Transport.send(message);
            System.out.println("[Mail] Email sent successfully to: " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("[Mail] Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendSMS(String phoneNumber, String messageText)
    {
        try {
            Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
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
            sendSMS(phone, message);
        }
    }

    public static void sendRegistrationAlert(String email, String phone, String firstName) {

        String subject = "GCM Registration";
        String message = "Hello " + firstName + ",\n\n" +
                "Welcome to GCM Services!\n" +
                "We're so excited you joined us.";

        sendEmail(email, subject, message);
        if (phone != null && !phone.isEmpty()) {
            sendSMS(phone, message);
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