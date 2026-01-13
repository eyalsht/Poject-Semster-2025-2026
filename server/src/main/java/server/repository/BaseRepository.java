package server.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import server.HibernateUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic base repository providing common CRUD operations.
 * All specific repositories extend this class.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public abstract class BaseRepository<T, ID> {

    protected final Class<T> entityClass;

    protected BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    // ==================== SESSION HELPERS ====================

    /**
     * Execute an operation within a transaction (for write operations).
     */
    protected void executeInTransaction(Consumer<Session> action) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            action.accept(session);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a query and return a result (for read operations).
     */
    protected <R> R executeQuery(Function<Session, R> query) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return query.apply(session);
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Find entity by ID.
     */
    public Optional<T> findById(ID id) {
        return executeQuery(session -> Optional.ofNullable(session.get(entityClass, id)));
    }

    /**
     * Get all entities.
     */
    public List<T> findAll() {
        return executeQuery(session ->
            session.createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                   .getResultList()
        );
    }

    /**
     * Save a new entity.
     */
    public void save(T entity) {
        executeInTransaction(session -> session.persist(entity));
    }

    /**
     * Update an existing entity.
     */
    public void update(T entity) {
        executeInTransaction(session -> session.merge(entity));
    }

    /**
     * Save or update an entity.
     */
    public T saveOrUpdate(T entity) {
        final Object[] result = new Object[1];
        executeInTransaction(session -> {
            result[0] = session.merge(entity);
        });
        return (T) result[0];
    }

    /**
     * Delete an entity.
     */
    public void delete(T entity) {
        executeInTransaction(session -> {
            T managed = session.merge(entity);
            session.remove(managed);
        });
    }

    /**
     * Delete entity by ID.
     */
    public void deleteById(ID id) {
        executeInTransaction(session -> {
            T entity = session.get(entityClass, id);
            if (entity != null) {
                session.remove(entity);
            }
        });
    }

    /**
     * Count all entities.
     */
    public long count() {
        return executeQuery(session ->
            session.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                   .uniqueResult()
        );
    }

    /**
     * Check if entity exists by ID.
     */
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }
}
