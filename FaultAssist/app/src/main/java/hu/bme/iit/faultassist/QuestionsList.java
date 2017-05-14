package hu.bme.iit.faultassist;

import java.util.ArrayList;
import java.util.List;

public class QuestionsList {
    List<QuestionElement> elements = new ArrayList<>();

    public void add(String id, String type, String question, String expected, String top_interval,
                    String bottom_interval, String solution, String unit) {
        QuestionElement element = new QuestionElement();
        element.id = id;
        element.type = type;
        element.question = question;
        element.expected = expected;
        element.top_interval = top_interval;
        element.bottom_interval = bottom_interval;
        element.solution = solution;
        element.unit = unit;
        elements.add(element);
    }

    public QuestionElement get(int pos){
        return elements.get(pos);
    }
}
