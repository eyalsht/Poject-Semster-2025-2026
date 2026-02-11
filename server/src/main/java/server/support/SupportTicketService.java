package server.support;

import common.enums.SupportTicketStatus;
import common.support.CreateSupportTicketRequest;
import common.support.ReplySupportTicketRequest;
import common.support.SupportTicket;
import common.support.SupportTicketRowDTO;
import common.user.Employee;
import common.user.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import server.HibernateUtil;

import java.time.LocalDateTime;
import java.util.List;

public class SupportTicketService {

    public static int createTicket(CreateSupportTicketRequest req) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = session.get(User.class, req.getUserId());
            if (user == null) {
                tx.rollback();
                throw new RuntimeException("User not found");
            }

            SupportTicket t = new SupportTicket(user, req.getTopic(), req.getText());
            session.persist(t);

            tx.commit();
            return t.getId();
        }
    }

    public static List<SupportTicketRowDTO> listAllTicketsForAgent(int agentId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // OPEN first (oldest->newest), DONE last (oldest->newest)
            List<SupportTicket> rows = session.createQuery(
                    "select t from SupportTicket t " +
                            "join fetch t.client c " +
                            "left join fetch t.agent a " +
                            "order by t.status asc, t.createdAt asc", SupportTicket.class
            ).list();

            return rows.stream().map(SupportTicketService::toRowDTO).toList();
        }
    }

    public static void replyToTicket(ReplySupportTicketRequest req) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            Employee agent = session.get(Employee.class, req.getAgentId());
            SupportTicket t = session.get(SupportTicket.class, req.getTicketId());

            if (agent == null) {
                tx.rollback();
                throw new RuntimeException("Agent not found");
            }
            if (t == null) {
                tx.rollback();
                throw new RuntimeException("Ticket not found");
            }

            // if already DONE -> do nothing (or throw)
            if (t.getStatus() == SupportTicketStatus.DONE) {
                tx.commit();
                return;
            }

            t.setAgent(agent);
            t.setAgentReply(req.getReplyText());
            t.setRepliedAt(LocalDateTime.now());
            t.setStatus(SupportTicketStatus.DONE);
            t.setReadByClient(false);

            session.merge(t);
            tx.commit();
        }
    }

    public static List<SupportTicketRowDTO> listRepliesForClient(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<SupportTicket> rows = session.createQuery(
                            "select t from SupportTicket t " +
                                    "join fetch t.client c " +
                                    "left join fetch t.agent a " +
                                    "where c.id = :uid and t.status = :done " +
                                    "order by t.repliedAt desc", SupportTicket.class
                    ).setParameter("uid", userId)
                    .setParameter("done", SupportTicketStatus.DONE)
                    .list();

            return rows.stream().map(SupportTicketService::toRowDTO).toList();
        }
    }

    public static void markRead(int userId, int ticketId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            SupportTicket t = session.get(SupportTicket.class, ticketId);
            if (t == null) {
                tx.rollback();
                throw new RuntimeException("Ticket not found");
            }
            if (t.getClient() == null || t.getClient().getId() != userId) {
                tx.rollback();
                throw new RuntimeException("Not your ticket");
            }

            t.setReadByClient(true);
            session.merge(t);

            tx.commit();
        }
    }

    private static SupportTicketRowDTO toRowDTO(SupportTicket t) {

        // FULL client message (used in agent reply window + client "open message" window)
        String fullClientText = t.getClientText();
        if (fullClientText == null) fullClientText = "";

        // PREVIEW (used in tables)
        String preview = fullClientText.replace("\n", " ");
        if (preview.length() > 60) preview = preview.substring(0, 60) + "...";

        String username = (t.getClient() != null ? t.getClient().getUsername() : "unknown");

        // agent name + reply (used in client inbox + also in agent dialog for DONE tickets)
        String agentName = "Support";
        if (t.getAgent() != null) {
            String first = t.getAgent().getFirstName();
            String last = t.getAgent().getLastName();
            String full = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            agentName = full.isBlank() ? t.getAgent().getUsername() : full;
        }

        String agentReply = t.getAgentReply();

        // ✅ IMPORTANT:
        // This assumes you updated SupportTicketRowDTO to include `clientText` (full message)
        // constructor: (..., preview, clientText, agentName, agentReply)
        return new SupportTicketRowDTO(
                t.getId(),
                username,
                t.getTopic(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getRepliedAt(),
                t.isReadByClient(),
                preview,
                fullClientText,   // ✅ full message sent to client/agent
                agentName,
                agentReply
        );
    }
}
