package com.titammods.quantumcubes.dimension;

public enum CubeLayout {
    LEGACY("legacy"),
    CHUNK("chunk");

    private final String id;

    CubeLayout(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static CubeLayout byId(String id) {
        for (CubeLayout layout : values()) {
            if (layout.id.equals(id)) {
                return layout;
            }
        }
        return LEGACY;
    }
}