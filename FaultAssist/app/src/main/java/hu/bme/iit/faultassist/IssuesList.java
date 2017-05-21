package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;

/** Used to add to a list the elements from the the issues table. **/
public class IssuesList {
    List<IssueElement> elements = new ArrayList<>();

    /** Simple function to add elements to the list. Making a IssueElement **/
    public void add (String id, String cause){
        IssueElement element = new IssueElement();
        element.id=id;
        element.cause=cause;
        elements.add(element);
    }
}
