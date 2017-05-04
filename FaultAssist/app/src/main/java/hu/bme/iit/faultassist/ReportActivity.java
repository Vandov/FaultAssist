package hu.bme.iit.faultassist;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, Callback {
    private ZXingScannerView scannerView;
    Button scan;
    Button proceed;
    TextView textView;
    String text;
    static ReportRegister reportRegister;
    ProgressDialog progressDialog;

    static JsonPrimitive username;
    static JsonPrimitive pass;
    static JsonPrimitive command;
    static JsonPrimitive table;
    static JsonPrimitive values;
    static JsonPrimitive selection;
    static JsonPrimitive specification;

    String json;
    static JsonObject jobj;

    static List<String> fields;
    static String[] machines = {"type"};
    static String[] issues = {"id", "cause", "status"};
    static String[] questions = {"id", "question", "question_type", "expected", "value_type", "interval_bottom", "interval_top", "leaf_solution"};
    static String machineType;
    private String currentNodeID;
    private boolean solved = false;
    List<ReturnValues> list;
    static int questionNum = 0;
    static int requestNum = 0;
    static int question_helper_num = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        config();
        Networking.initialize();
    }

    private void config() {
        scan = (Button) findViewById(R.id.scan_btn_report);
        proceed = (Button) findViewById(R.id.proceed_btn_report);
        textView = (TextView) findViewById(R.id.text_view_report);

        textView.setText(text);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        username = new JsonPrimitive(getIntent().getStringExtra("username"));
        pass = new JsonPrimitive(getIntent().getStringExtra("password"));

        reportRegister = new ReportRegister();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerView = new ZXingScannerView(getApplicationContext());
                setContentView(scannerView);
                scannerView.setResultHandler(ReportActivity.this);
                scannerView.startCamera();
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text != null) {

                    json = SQL("select", "machines", "", "type", "1");

                    try {
                        if (reportRegister.isEmpty()) {
                            progressDialog.setMessage("Loading");
                            showDialog();

                            machineType = "";
                            currentNodeID = "";
                            updateFields(machines);

                            Networking.post(Networking.query_link, json, ReportActivity.this);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    toast("Please scan the QR code of the machine before continuing");
                }
            }
        });
    }

    public static void updateFields(String[] strings) {
        fields = new ArrayList<>();
        for (int i = 0; i < strings.length; i++) {
            fields.add(strings[i]);
        }
    }

    public static String SQL(String mCommand, String mTable, String mValues, String mSelection, String mSpecification) {

        command = new JsonPrimitive(mCommand);
        table = new JsonPrimitive(mTable);
        values = new JsonPrimitive(mValues);
        selection = new JsonPrimitive(mSelection);
        specification = new JsonPrimitive(mSpecification);


        JsonObject object = new JsonObject();

        object.add("username", username);
        object.add("password", pass);
        object.add("command", command);
        object.add("table", table);
        object.add("values", values);
        object.add("selection", selection);
        object.add("specification", specification);

        jobj = object;

        return object.toString();
    }

    @Override
    public void handleResult(Result result) {
        text = "'" + result.getText() + "' machine recognised. If that's correct please proceed with your report.";
        specification = new JsonPrimitive(result.getText());
        scannerView.stopCamera();
        setContentView(R.layout.activity_report);
        config();
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressDialog.hide();
        try {
            scannerView.stopCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Something went wrong!");
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        hideDialog();

        String jsonData = URLDecoder.decode(response.body().string(), "UTF-8");

        JSONArray Jarray = null;
        list = new ArrayList<>();
        String question_type = "";
        String question_found = "";

        try {
            Jarray = new JSONArray(jsonData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Jarray != null) {
            for (int i = 0; i < Jarray.length(); i++) {
                try {

                    ReturnValues values = new ReturnValues();

                    for (int j = 0; j < fields.size(); j++) {
                        values.add(fields.get(j), Jarray.getJSONObject(i).get(fields.get(j)).toString());
                        if (fields.get(j).equals("type") && reportRegister.isEmpty()) {
                            machineType = Jarray.getJSONObject(i).getString("type");
                        }
                        if (fields.get(j).equals("id")) {
                            currentNodeID = Jarray.getJSONObject(i).getString("id");
                        }
                        if (fields.get(j).equals("question_type")) {
                            question_type = Jarray.getJSONObject(i).getString("question_type");
                        }
                        if (fields.get(j).equals("question")) {
                            question_found = Jarray.getJSONObject(i).getString("question");
                        }
                    }
                    list.add(values);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            requestNum++;
            System.out.println(Jarray.toString());
            System.out.println(requestNum);

            if (!solved) {
                String name = "";
                String type = question_type;
                String question = question_found;

                if (requestNum % 2 == 0 && requestNum > 2) {
                    try {
                        type = Jarray.getJSONObject(0).getString("question_type");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                String[] s = nextStep(name, type, question);

                name = s[0];
                type = s[1];
                question = s[2];

                if (!reportRegister.isEmpty() && requestNum % 2 == 0) {
                    displayDialog(name, type, question);
                    report(json, type, jobj);
                } else if (reportRegister.isEmpty()) {
                    report(json, "head", jobj);
                }
            }
        }
    }

    private void displayDialog(String name, String type, String question) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment dialog = ReportDialog.newInstance(name, type, question, list);
        dialog.show(ft, "dialog");
    }

    private String[] nextStep(String name, String type, String question) {

        if (reportRegister.isEmpty()) {
            updateFields(issues);
            json = SQL("select", machineType + "_ISSUES", "", "*", "status = 'Active' AND (id NOT REGEXP '[0-9]+[_]')");
            try {
                Networking.post(Networking.query_link, json, ReportActivity.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (reportRegister.events.size() == 1) {
            name = "Pick the reported issue";
            type = "pick";
            questionNum = 0;
        } else if (requestNum % 2 == 1) {

            updateFields(questions);
            String temp = "id = '" + currentNodeID + "'";

            json = SQL("select", machineType + "_QUESTIONS", "", "*", temp);
            System.out.println(json);
            try {
                Networking.post(Networking.query_link, json, ReportActivity.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            questionNum++;
        } else {
            name = "Question " + questionNum;
        }
        String[] s = {name, type, question};
        return s;
    }

    private void showDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void toast(final String s) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void report(String json, String type, JsonObject jobj) {
        ReportEvent event = new ReportEvent();
        event.json = json;
        event.type = type;
        event.jobj = jobj;
        reportRegister.push(event);
    }

    public static void resendLastRequest(ReportActivity activity) {
        String prev = reportRegister.events.get(reportRegister.events.size()-1).json;
        try {
            Networking.post(Networking.query_link, prev, activity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
