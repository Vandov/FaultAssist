package hu.bme.iit.faultassist;

import java.util.HashMap;

/** Storing the values in a HashMap. Functions to get back certain values and insert some values in the map. **/
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
