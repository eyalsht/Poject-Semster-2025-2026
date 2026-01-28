package server.support;

import common.content.City;
import common.purchase.Subscription;
import common.support.SupportChoiceDTO;
import common.support.SupportSubmitRequest;
import common.support.SupportSubmitResponse;
import common.user.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import server.HibernateUtil;

import java.util.*;
import java.util.stream.Collectors;

public class SupportService {

    public static SupportSubmitResponse handleSupport(SupportSubmitRequest req) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = session.get(User.class, req.getUserId());
            if (user == null) {
                tx.rollback();
                return new SupportSubmitResponse(true, "User not found.", null);
            }

            // ---- Topic 1: membership expiry ----
            if ("MEMBERSHIP_EXPIRE".equals(req.getTopic())) {

                // Query all subscriptions for this user
                List<Subscription> subs = session.createQuery(
                        "select s from Subscription s where s.user.id = :uid",
                        Subscription.class
                ).setParameter("uid", user.getId()).list();

                if (subs.isEmpty()) {
                    tx.commit();
                    return new SupportSubmitResponse(true,
                            "I checked your account: you have no subscriptions.",
                            null);
                }

                // If cityId chosen -> answer for that city
                if (req.getCityId() != null) {
                    Subscription best = subs.stream()
                            .filter(s -> s.getCity() != null && s.getCity().getId() == req.getCityId())
                            .max(Comparator.comparing(Subscription::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);

                    if (best == null || best.getExpirationDate() == null) {
                        tx.commit();
                        return new SupportSubmitResponse(true,
                                "I couldn't find subscription info for that city.",
                                null);
                    }

                    City c = best.getCity();
                    tx.commit();
                    return new SupportSubmitResponse(true,
                            "Your subscription for " + c.getName() + " is valid until: " + best.getExpirationDate(),
                            null);
                }

                // Otherwise: if exactly one city subscription -> answer directly
                // Group by city
                Map<Integer, List<Subscription>> byCity = subs.stream()
                        .filter(s -> s.getCity() != null)
                        .collect(Collectors.groupingBy(s -> s.getCity().getId()));

                if (byCity.size() == 1) {
                    Subscription best = byCity.values().iterator().next().stream()
                            .max(Comparator.comparing(Subscription::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);

                    City c = best.getCity();
                    tx.commit();
                    return new SupportSubmitResponse(true,
                            "Your subscription for " + c.getName() + " is valid until: " + best.getExpirationDate(),
                            null);
                }

                // Multiple cities -> return choices
                List<SupportChoiceDTO> choices = byCity.values().stream()
                        .map(list -> list.get(0).getCity())
                        .filter(Objects::nonNull)
                        .map(c -> new SupportChoiceDTO(c.getId(), c.getName()))
                        .sorted(Comparator.comparing(SupportChoiceDTO::getLabel))
                        .collect(Collectors.toList());

                tx.commit();
                return new SupportSubmitResponse(false,
                        "You have multiple subscriptions. Choose a city:",
                        choices);
            }

            tx.commit();

            String text = req.getMessageText() == null ? "" : req.getMessageText().trim();
            if (!text.isEmpty()) {
                // Create real support ticket
                server.support.SupportTicketService.createTicket(
                        new common.support.CreateSupportTicketRequest(req.getUserId(), req.getTopic(), text)
                );
            }

            return new SupportSubmitResponse(false,
                    "I couldn't answer this automatically. Your request was forwarded to support.",
                    null);

        }
    }
}
