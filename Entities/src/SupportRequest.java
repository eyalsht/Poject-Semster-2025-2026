package entities;

import java.time.LocalDate;
public class SupportRequest
{
    private int id;
    private String subject;
    private String description;
    private LocalDate date;
    private LocalDate dateOpened ;
    private RequestStatus status;
    private String response;

    public SupportRequest(int id, String subject, String description) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.date = LocalDate.now();
        this.status = RequestStatus.OPEN;
        this.response = null;
    }


    public void closeRequest() {
        this.status = RequestStatus.CLOSED;
    }

    public void setResponse(String answer) {
        this.response = answer;
    }

    // getters (usually expected even if not drawn)
    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDateOpened() {
        return date;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }
}
