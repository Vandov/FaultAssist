package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;

public class IssuesList {
    List<IssueElement> elements = new ArrayList<>();

    public void add (String id, String cause){
        IssueElement element = new IssueElement();
        element.id=id;
        element.cause=cause;
        elements.add(element);
    }
}
