package common;

public abstract class ContentItem {
    protected int id;
    protected String name;
    protected String description;

    public ContentItem() {
        this(0, null, null);
    }

    public ContentItem(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getID() {
        return this.id;
    }
    public void setID(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public abstract String getDetails();
}
