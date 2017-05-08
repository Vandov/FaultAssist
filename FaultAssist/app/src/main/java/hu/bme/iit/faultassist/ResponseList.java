package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;

public class ResponseList {
    List<ResponseListElement> list = new ArrayList<>();

    public void add(String id){
        ResponseListElement element = new ResponseListElement();
        element.id=id;
        list.add(element);
    }
}
