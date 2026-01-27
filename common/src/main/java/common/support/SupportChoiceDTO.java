package common.support;

import java.io.Serializable;

public class SupportChoiceDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int cityId;
    private final String label;

    public SupportChoiceDTO(int cityId, String label) {
        this.cityId = cityId;
        this.label = label;
    }

    public int getCityId() { return cityId; }
    public String getLabel() { return label; }
}
