package common.support;

import common.user.Employee;
import common.user.User;
import jakarta.persistence.*;
import common.enums.SupportTicketStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // who opened the ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(name = "topic", length = 60, nullable = false)
    private String topic;

    @Lob
    @Column(name = "client_text", nullable = false)
    private String clientText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    // who answered (support agent)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Employee agent;

    @Lob
    @Column(name = "agent_reply")
    private String agentReply;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "read_by_client", nullable = false)
    private boolean readByClient = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = SupportTicketStatus.OPEN;
    }

    public SupportTicket() {}

    public SupportTicket(User client, String topic, String clientText) {
        this.client = client;
        this.topic = topic;
        this.clientText = clientText;
        this.status = SupportTicketStatus.OPEN;
    }

    // ===== getters/setters =====
    public int getId() { return id; }
    public User getClient() { return client; }
    public String getTopic() { return topic; }
    public String getClientText() { return clientText; }
    public SupportTicketStatus getStatus() { return status; }
    public Employee getAgent() { return agent; }
    public String getAgentReply() { return agentReply; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public boolean isReadByClient() { return readByClient; }

    public void setStatus(SupportTicketStatus status) { this.status = status; }
    public void setAgent(Employee agent) { this.agent = agent; }
    public void setAgentReply(String agentReply) { this.agentReply = agentReply; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
    public void setReadByClient(boolean readByClient) { this.readByClient = readByClient; }
}
