package qengine.program.Models;

import java.util.HashMap;

public class Dictionary extends HashMap<String, Integer> {

    private int nextId = 0;

    public int put(String value) {
        putIfAbsent(value, nextId);
        return nextId++;
    }
}
