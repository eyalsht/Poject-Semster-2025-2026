package server.report;

import org.hibernate.SessionFactory;

public class ReportRequestContext
{

    private final SessionFactory sessionFactory;

    public ReportRequestContext(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory()
    {
        return sessionFactory;
    }
}
