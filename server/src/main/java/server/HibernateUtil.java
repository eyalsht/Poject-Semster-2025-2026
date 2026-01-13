package server;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for managing the Hibernate SessionFactory.
 * 
 * This replaces the old DBController.connectToDB() approach.
 * The SessionFactory is thread-safe and should be created once at application startup.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    /**
     * Initializes the SessionFactory from hibernate configuration file.
     * Call this once at application startup.
     * 
     * @param configFile the name of the hibernate config file (e.g., "dbHibernate.cfg.xml")
     */
    public static void initialize(String configFile) {
        if (sessionFactory != null) {
            System.out.println("HibernateUtil: SessionFactory already initialized.");
            return;
        }

        try {
            System.out.println("HibernateUtil: Initializing SessionFactory from " + configFile + "...");
            
            // Create Configuration from the XML file
            Configuration configuration = new Configuration();
            configuration.configure(configFile);  // Loads from classpath (resources folder)
            
            // Build the SessionFactory
            sessionFactory = configuration.buildSessionFactory();
            
            System.out.println("HibernateUtil: SessionFactory created successfully!");
            
        } catch (Exception e) {
            System.err.println("HibernateUtil: Failed to create SessionFactory!");
            e.printStackTrace();
            throw new ExceptionInInitializerError("Failed to initialize Hibernate: " + e.getMessage());
        }
    }

    /**
     * Initializes with the default config file name.
     */
    public static void initialize() {
        initialize("dbHibernate.cfg.xml");
    }

    /**
     * Returns the SessionFactory.
     * 
     * @return the SessionFactory
     * @throws IllegalStateException if not initialized
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException(
                "SessionFactory is not initialized! Call HibernateUtil.initialize() first."
            );
        }
        return sessionFactory;
    }

    /**
     * Closes the SessionFactory and releases all resources.
     * Call this when shutting down the application.
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            System.out.println("HibernateUtil: Shutting down SessionFactory...");
            sessionFactory.close();
            sessionFactory = null;
            System.out.println("HibernateUtil: SessionFactory closed.");
        }
    }

    /**
     * Check if the SessionFactory is initialized and open.
     * 
     * @return true if ready to use
     */
    public static boolean isInitialized() {
        return sessionFactory != null && !sessionFactory.isClosed();
    }
}
