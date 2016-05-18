package com.yarieldis.sqlike.base;

import java.io.Serializable;

public class Entity implements Serializable {

    private int id;

    public Entity() {
        id = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
