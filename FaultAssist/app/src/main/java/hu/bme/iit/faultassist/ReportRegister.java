package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;

public class ReportRegister {
    List<ReportEvent> events = new ArrayList<>();

    public void push(ReportEvent reportEvent){
        events.add(reportEvent);
    }

    public void pop(){
        events.remove(events.size()-1);
    }
}
