package common.enums;

import java.io.Serializable;

/**
 * Determines what a user can see in the map content popup.
 */
public enum MapAccessLevel implements Serializable {
    FULL_ACCESS,    // Employee or active Subscriber -> image + sites + tours
    MAP_PURCHASED,  // One-time buyer -> image + sites, NO tours tab
    NO_ACCESS       // Guest or non-buyer -> site list only, NO image (placeholder)
}
