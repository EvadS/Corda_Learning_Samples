package com.safexain.nodesharing.model;

import java.util.HashMap;
import java.util.Map;

public enum NodeInfoContainerType {
    NODE_INFO(1),
    UPDATE_CORDAPP(2);

    private int value;
    private static Map map = new HashMap<>();

    private NodeInfoContainerType(int value) {
        this.value = value;
    }

    static {
        for (NodeInfoContainerType nodeInfoContainerType : NodeInfoContainerType.values()) {
            map.put(nodeInfoContainerType.value, nodeInfoContainerType);
        }
    }

    public static NodeInfoContainerType valueOf(int pageType) {
        return (NodeInfoContainerType) map.get(pageType);
    }

    public int getValue() {
        return value;
    }
}
