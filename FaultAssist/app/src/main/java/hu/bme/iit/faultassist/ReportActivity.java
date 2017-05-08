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
import com.google.zxing.common.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private boolean solved = false;
    //List<ReturnValues> list;

    static int requestNum = 0;
    static int question_helper_num = 1;

    /**********************************************/
    List<IssuesList> responseLists = new ArrayList<>();
    static QuestionsList questionList = new QuestionsList();
    private String machineID = "";
    private String machineType = "";
    static String currentID = "";
    private String headID = "";
    private String issuesSelection = "id, cause, status";
    private String questionSelection = "id, question, question_type, expected, value_type, interval_bottom, interval_top, leaf_solution";
    static int questionNum = 0;

    private enum State {
        INITIAL,
        TYPE,
        HEADS,
        ISSUES,
        QUESTION,
    }

    public static State state;


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

        state = State.INITIAL;

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
                    nextStep();
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
        System.out.println(jobj.toString());
        return object.toString();
    }

    @Override
    public void handleResult(Result result) {
        machineID = result.getText();
        text = "'" + machineID + "' machine recognised. If that's correct please proceed with your report.";
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

        try {
            Jarray = new JSONArray(jsonData);
            System.out.println(Jarray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (state == State.ISSUES) {
            IssuesList list = new IssuesList();

            if (Jarray != null) {
                for (int i = 0; i < Jarray.length(); i++) {
                    try {
                        list.add(Jarray.getJSONObject(i).get("id").toString(), Jarray.getJSONObject(i).getString("cause").toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                responseLists.add(0, list);
            }
        } else if (state == State.TYPE) {
            if (Jarray != null) {
                try {
                    machineType = Jarray.getJSONObject(0).get("type").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (state == State.HEADS) {
            List<ReturnValues> list = new ArrayList<>();

            if (Jarray != null) {
                for (int i = 0; i < Jarray.length(); i++) {
                    try {
                        ReturnValues values = new ReturnValues();
                        values.add("cause", Jarray.getJSONObject(i).get("cause").toString());
                        values.add("id", Jarray.getJSONObject(i).get("id").toString());
                        list.add(values);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            displayDialog("Pick reported issue", "pick", "", list);
        } else if (state == State.QUESTION) {
            if (Jarray != null) {
                try {

                    questionList.add(Jarray.getJSONObject(0).get("id").toString(),
                            Jarray.getJSONObject(0).get("question_type").toString(),
                            Jarray.getJSONObject(0).get("question").toString(),
                            Jarray.getJSONObject(0).get("expected").toString(),
                            Jarray.getJSONObject(0).get("interval_top").toString(),
                            Jarray.getJSONObject(0).get("interval_bottom").toString(),
                            Jarray.getJSONObject(0).get("leaf_solution").toString(),
                            Jarray.getJSONObject(0).get("value_type").toString());


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


        nextStep();
    }

    private void nextStep() {
        System.out.println(state);
        switch (state) {
            case INITIAL:
                try {
                    state = State.TYPE;
                    Networking.post(Networking.query_link, SQL("select", "machines", "", "type", "id = '" + machineID + "'"), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case TYPE:
                try {
                    state = State.HEADS;
                    Networking.post(Networking.query_link, SQL("select", machineType + "_ISSUES", "", issuesSelection, "status = 'Active' AND " + generateSelectID()), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case HEADS:
                state = State.ISSUES;
                break;
            case ISSUES:
                state = State.QUESTION;
                questionNum = 1;
                getNextQuestion();
                break;
            case QUESTION:
                askQuestion();
                break;
        }
    }

    public void answerProcessBoolean(boolean bool) {
        questionList.get(questionNum - 1).answer = questionList.get(questionNum - 1).expected;
        if (bool) {
            moveForward();
        } else {
            moveSide();
        }
    }

    public void answerProcessNumber(boolean bool, String number) {
        questionList.get(questionNum - 1).answer = number;
        if (bool) {
            moveForward();
        } else {
            moveSide();
        }
    }

    private void moveForward() {
        if (questionList.get(questionNum - 1).solution.length() > 0) {
            /**** give solution ***/
        }
        currentID = questionList.get(questionNum - 1).id;
        questionNum++;
        getNextQuestion();
    }

    private void moveSide() {
        questionNum++;
        getNextQuestion();
    }

    private void moveBack() {
        rollBackID();
        questionNum++;

    }

    /****
     * TEST
     ****/
    private void rollBackID() {
        String[] s = currentID.split("_");
        Arrays.copyOf(s, s.length - 1);
        for (int i = 0; i < s.length - 2; i++) {
            s[i] = s[i] + "_";
        }

    }

    private void askQuestion() {
        QuestionElement question = questionList.get(questionNum - 1);
        displayDialog("Question " + questionNum + ":", question.type, question.question, null);
    }

    private String generateSelectID() {
        if (state == State.HEADS) {
            return "(id NOT REGEXP '[0-9]+[_]')";
        } else if (state == State.ISSUES) {
            return "(id REGEXP '" + currentID + "[_]')";
        } else {
            return null;
        }
    }

    private void getNextQuestion() {
        int pos = getQuestionPos(getNextRegex());
        if (pos >= 0) {
            IssueElement element = responseLists.get(0).elements.get(pos);
            try {
                Networking.post(Networking.query_link, SQL("select", machineType + "_QUESTIONS", "", questionSelection, "id = '" + element.id + "'"), this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (currentID != machineID) {
            moveBack();
        } else {
            toast("ENDING: call for help");
        }
    }

    private int getQuestionPos(String regex) {
        Pattern p = Pattern.compile(regex);
        for (int i = 0; i < responseLists.get(0).elements.size(); i++) {
            if (!responseLists.get(0).elements.get(i).visited) {
                Matcher m = p.matcher(responseLists.get(0).elements.get(i).id);
                System.out.println(responseLists.get(0).elements.get(i).id);
                if (m.find()) {
                    currentID = responseLists.get(0).elements.get(i).id;
                    responseLists.get(0).elements.get(i).visited = true;
                    System.out.println("match");
                    return i;
                }
            }
        }
        return -1;
    }

    private String getNextRegex() {
        String regex = "\\A" + currentID + "[_][0-9]+\\z";
        return regex;
    }

    public void picked(String id) {
        currentID = id;
        headID = id;
        try {
            Networking.post(Networking.query_link, SQL("select", machineType + "_ISSUES", "", issuesSelection, "status = 'Active' AND " + generateSelectID()), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayDialog(String name, String type, String question, List list) {
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

    private void report(String json, String type, JsonObject jobj) {
        ReportEvent event = new ReportEvent();
        event.json = json;
        event.type = type;
        event.jobj = jobj;
        reportRegister.push(event);
    }

    public static void resendLastRequest(ReportActivity activity) {
        String prev = reportRegister.events.get(reportRegister.events.size() - 1).json;
        try {
            Networking.post(Networking.query_link, prev, activity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
