package com.lemon.player.model;

/**
 * An interface used to define sorting keys used for object comparison
 */
public interface Sortable {

    String getSortKey();

    void setSortKey();

}