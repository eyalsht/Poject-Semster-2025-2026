package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class LogMapViewHandler implements RequestHandler {
    private final SessionFactory sf;
    public LogMapViewHandler(SessionFactory sf) { this.sf = sf; }

    @Override
    public Message handle(Message request) {
        ArrayList<Object> data = (ArrayList<Object>) request.getMessage();
        Integer userId = (Integer) data.get(0);
        Integer cityId = (Integer) data.get(1);
        Integer mapId  = (Integer) data.get(2); // can be null

        try (Session s = sf.openSession()) {
            s.beginTransaction();

            s.createNativeQuery("""
                INSERT INTO map_view_events(city_id, map_id, user_id, viewed_at)
                VALUES (:cityId, :mapId, :userId, :ts)
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("mapId", mapId)
                    .setParameter("userId", userId)
                    .setParameter("ts", LocalDateTime.now())
                    .executeUpdate();

            s.getTransaction().commit();
        }

        return new Message(ActionType.LOG_MAP_VIEW_RESPONSE, true);
    }
}
