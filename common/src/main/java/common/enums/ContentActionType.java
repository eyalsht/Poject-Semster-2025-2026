package common.enums;

import java.io.Serializable;

/**
 * Types of content changes that require approval.
 */
public enum ContentActionType implements Serializable {
    ADD,
    EDIT,
    DELETE
}
