package entities;

public abstract class ContentItem {
    protected int ID; // [cite: 66]
    protected String name; // [cite: 67]
    protected String description; // [cite: 68]

    public abstract String getDetails();
}