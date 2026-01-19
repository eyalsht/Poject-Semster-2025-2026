package common.content;

import jakarta.persistence.*;
import java.io.Serializable;

@MappedSuperclass  // Not an entity itself, but provides common mappings to subclasses
public abstract class ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;

    @Column(name = "name")
    protected String name;

    @Column(name = "description", columnDefinition = "TEXT")
    protected String description;

    public ContentItem() {
        this(0, null, null);
    }

    public ContentItem(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public ContentItem(String name) {
        this.name = name;
        this.id = 0;
    }

    public int getID() { return this.id; }
    public void setID(int id) { this.id = id; }
    
    // getId() for JPA compatibility
    public int getId() { return this.id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public abstract String getDetails();
}