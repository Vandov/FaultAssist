package hu.bme.iit.faultassist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ReportEventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        config(savedInstanceState);
    }

    private void config(Bundle bundle) {
        String type = bundle.getString("eventType");
        switch (type){
            case "BOOLEAN":
        }
    }

}
