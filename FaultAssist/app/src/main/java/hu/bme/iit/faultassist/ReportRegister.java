package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;
import okhttp3.Response;

public class ReportRegister {
    List<ReportEvent> events = new ArrayList<>();

    public void push(ReportEvent reportEvent){
        events.add(reportEvent);
    }

    public ReportEvent pop(){
        ReportEvent event = events.get(events.size()-1);
        events.remove(events.size()-1);
        return event;
    }

    public boolean isEmpty(){
        return events.isEmpty();
    }
}
