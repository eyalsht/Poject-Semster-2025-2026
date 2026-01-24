package common.enums;

public enum SiteDuration
{
    MIN_30("30 minutes",0.5),
    HOUR_1("1 hour",1),
    HOUR_1_5("1.5 hours",1.5),
    HOUR_2("2 hours",2),
    HOUR_2_5("2.5 hours",2.5),
    HOUR_3("3 hours",3),
    HOUR_3_5("3.5 hours",3.5),
    HOUR_4("4 hours",4);

    private final String label;
    private final double hours;

    SiteDuration(String label, double minutes)
    {
        this.label = label;
        this.hours = minutes;
    }
    public double getHours() {
        return hours;
    }
    @Override
    public String toString() {
        return label;  // What the ComboBox shows
    }
    public static SiteDuration fromLabel(String label)
    {
        for (SiteDuration duration : SiteDuration.values()) {
            if (duration.label.equalsIgnoreCase(label)) {
                return duration;
            }
        }
        return null;
    }
}
