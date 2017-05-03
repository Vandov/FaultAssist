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
    ReportRegister reportRegister;
    ProgressDialog progressDialog;

    JsonPrimitive username;
    JsonPrimitive pass;
    JsonPrimitive command;
    JsonPrimitive table;
    JsonPrimitive values;
    JsonPrimitive selection;
    JsonPrimitive specification;

    String json;

    List<String> fields;
    String[] machines = {"type"};
    String[] issues = {"id", "cause", "status"};
    String[] questions = {"id", "question", "question_type", "expected", "value_type", "interval_bottom", "interval_top", "leaf_solution"};
    private String machineType;
    private String currentNodeID;
    private boolean solved = false;
    List<ReturnValues> list;

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

    private void updateFields(String[] strings) {
        fields = new ArrayList<>();
        for (int i = 0; i < strings.length; i++) {
            fields.add(strings[i]);
        }
    }

    private String SQL(String mCommand, String mTable, String mValues, String mSelection, String mSpecification) {

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

        return object.toString();
    }

    @Override
    public void handleResult(Result result) {
        text = "'" + result.getText().toString() + "' machine recognised. If that's correct please proceed with your report.";
        specification = new JsonPrimitive(result.getText().toString());
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

        try {
            Jarray = new JSONArray(jsonData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Jarray != null) {
            for (int i = 0; i < Jarray.length(); i++)
                try {

                    for (int j = 0; j < fields.size(); j++) {
                        ReturnValues values = new ReturnValues();
                        values.add(fields.get(j), Jarray.getJSONObject(j).get(fields.get(j)).toString());
                        list.add(values);
                        if (fields.get(j).equals("type") && reportRegister.isEmpty())
                            machineType = Jarray.getJSONObject(j).getString("type");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            if (!solved && reportRegister.isEmpty()) nextStep();
        }
    }

    private void nextStep() {

        String name = "";
        String type = "";
        String question = "";

        if (reportRegister.isEmpty()) {
            updateFields(issues);
            json = SQL("select", machineType + "_ISSUES", "", "id, cause, status", "(id REGEXP '[0-9].[_]')");//"id, cause, status", "status='Active'"
            try {
                Networking.post(Networking.query_link, json, ReportActivity.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            report(json, "pick");
            name = "Pick the reported issue";
            type = "pick";
        } else {

        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment dialog = ReportDialog.newInstance(name, type, question, list);
        dialog.show(ft, "dialog");
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

    private void report(String json, String type) {
        ReportEvent event = new ReportEvent();
        event.json = json;
        event.type = type;
        reportRegister.push(event);
    }
}
