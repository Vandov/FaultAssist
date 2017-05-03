package hu.bme.iit.faultassist;

import java.util.HashMap;

public class ReturnValues {
    HashMap<String, String> map = new HashMap<>();

    public void add(String key, String value) {
        map.put(key, value);
    }

    public String get(String key) {
        return map.get(key);
    }

    public int size() {
        return map.size();
    }
}
