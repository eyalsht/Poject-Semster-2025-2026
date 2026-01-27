package common.support;

import java.io.Serializable;
import java.util.List;

public class SupportSubmitResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean answeredByBot;
    private final String responseText;

    // If bot needs user to pick something (multiple subscriptions)
    private final List<SupportChoiceDTO> choices;

    public SupportSubmitResponse(boolean answeredByBot, String responseText, List<SupportChoiceDTO> choices) {
        this.answeredByBot = answeredByBot;
        this.responseText = responseText;
        this.choices = choices;
    }

    public boolean isAnsweredByBot() { return answeredByBot; }
    public String getResponseText() { return responseText; }
    public List<SupportChoiceDTO> getChoices() { return choices; }
}
