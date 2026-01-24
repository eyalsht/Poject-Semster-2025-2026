package common.report;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.List;

public class AllClientsReport implements Serializable {

    public static class MonthCount implements Serializable {
        public YearMonth month;
        public long count;

        public MonthCount() {}
        public MonthCount(YearMonth month, long count) {
            this.month = month;
            this.count = count;
        }
    }

    public static class ClientRow implements Serializable {
        public int userId;
        public String username;
        public String email;
        public String firstName;
        public String lastName;
        public java.time.LocalDateTime createdAt;

        public ClientRow() {}
        public ClientRow(int userId, String username, String email,
                         String firstName, String lastName,
                         java.time.LocalDateTime createdAt) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.createdAt = createdAt;
        }
    }

    public List<MonthCount> last5Months;
    public List<ClientRow> clientsNewestFirst;

    public AllClientsReport() {}
    public AllClientsReport(List<MonthCount> last5Months, List<ClientRow> clientsNewestFirst) {
        this.last5Months = last5Months;
        this.clientsNewestFirst = clientsNewestFirst;
    }
}
