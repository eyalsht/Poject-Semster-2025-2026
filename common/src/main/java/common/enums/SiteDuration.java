package common.enums;

public enum SiteDuration
{
    MIN_30("30 minutes"),
    HOUR_1("1 hour"),
    HOUR_1_5("1.5 hours"),
    HOUR_2("2 hours"),
    HOUR_2_5("2.5 hours"),
    HOUR_3("3 hours"),
    HOUR_3_5("3.5 hours"),
    HOUR_4("4 hours");

    private final String label;

    SiteDuration(String label) {
        this.label = label;
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
