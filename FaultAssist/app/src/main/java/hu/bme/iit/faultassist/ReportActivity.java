package hu.bme.iit.faultassist;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


import android.app.ProgressDialog;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import java.nio.DoubleBuffer;
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
    ProgressDialog progressDialog;

    static JsonPrimitive username;
    static JsonPrimitive pass;
    static JsonPrimitive command;
    static JsonPrimitive table;
    static JsonPrimitive values;
    static JsonPrimitive selection;
    static JsonPrimitive specification;

    static JsonObject jobj;
    List<String> report_list;

    static List<IssuesList> responseLists = new ArrayList<>();
    static QuestionsList questionList = new QuestionsList();
    private String machineID = "";
    private String machineType = "";
    static String currentID = "";
    private String headID = "";
    private String issuesSelection = "id, cause, status";
    private String questionSelection = "id, question, question_type, expected, value_type, interval_bottom, interval_top, leaf_solution";
    static int questionNum = 0;

    public enum State {
        INITIAL,
        TYPE,
        HEADS,
        ISSUES,
        QUESTION,
        SUGGESTION,
        REPORTING_START,
        REPORTING
    }

    public static State state;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        text = getResources().getString(R.string.report_info);
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
                        list.add(Jarray.getJSONObject(i).get("id").toString(), Jarray.getJSONObject(i).get("cause").toString());
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

            displayDialog("Pick reported issue", "pick", "", null, list);
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
            case SUGGESTION:
                showSuggestion();
                break;
        }
    }

    private void showSuggestion() {
        QuestionElement question = questionList.get(questionNum - 1);
        displayDialog("Suggestion", "suggestion", question.solution, null, null);
    }

    public void answerProcessBoolean(boolean bool) {
        if (bool) {
            questionList.get(questionNum - 1).answer = questionList.get(questionNum - 1).expected;
            moveForward();
        } else {
            /***********************************************************************************************************MEGVÁLTOZTATNI DB VÁLTOZÁS UTÁN****/
            questionList.get(questionNum - 1).answer = "";
            moveSide();
        }
    }

    public void solved(boolean b) {
        if (b) {
            String name = "";
            for (int i = 0; i < responseLists.get(0).elements.size(); i++) {
                if (questionList.get(ReportActivity.questionNum - 1).id.equals(responseLists.get(0).elements.get(i).id)) {
                    name = responseLists.get(0).elements.get(i).cause;
                }
            }
            displayDialog("Solved Issue", "solved", name, null, null);
        } else {
            state = State.QUESTION;
            moveSide();
        }
    }

    public void answerProcessNumber(String number) {
        questionList.get(questionNum - 1).answer = number;
        boolean bool = true;
        if (Double.valueOf(number) <= Double.valueOf(questionList.get(questionNum - 1).top_interval)
                && Double.valueOf(number) >= Double.valueOf(questionList.get(questionNum - 1).bottom_interval)) {
            bool = false;
        }
        if (bool) {
            moveForward();
        } else {
            moveSide();
        }
    }

    private void moveForward() {
        if (questionList.get(questionNum - 1).solution.length() > 0) {
            state = State.SUGGESTION;
            nextStep();
        } else {
            currentID = questionList.get(questionNum - 1).id;
            questionNum++;
            getNextQuestion();
        }
    }

    private void moveSide() {
        rollBackID();
        questionNum++;
        getNextQuestion();
    }

    private void moveBack() {
        rollBackID();
        getNextQuestion();
    }

    private void rollBackID() {
        try {
            questionList.get(questionNum - 1).answer = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] s = currentID.split("_");
        StringBuilder builder = new StringBuilder();
        s = Arrays.copyOf(s, s.length - 1);
        if (s.length > 0) {
            for (int i = 0; i < s.length; i++) {
                builder.append(s[i]);
                if (i < s.length - 1) {
                    builder.append("_");
                }
            }
            currentID = builder.toString();
        }
    }

    private void askQuestion() {
        QuestionElement question = questionList.get(questionNum - 1);
        if (question.type.trim().equalsIgnoreCase("boolean")) {
            questionList.get(questionNum - 1).answer = "";
        }
        displayDialog("Question " + questionNum + ":", question.type, question.question, question.value_type, null);
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
        } else if (!currentID.equals(headID)) {
            moveBack();
        } else {
            displayDialog("Couldn't solve issue", "failed", getResources().getString(R.string.failed), null, null);
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
        return "\\A" + currentID + "[_][0-9]+\\z";
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

    private void displayDialog(String name, String type, String text, String value_type, List list) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment dialog = ReportDialog.newInstance(name, type, text, value_type, list);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    /*************************************************************************************************************MAYBE IMPLEMENT****/
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

    public void report(boolean b) {
        List<String> report_route = new ArrayList<>();
        for (int i = 0; i < questionList.elements.size(); i++) {
            if (questionList.elements.get(i).type.trim().equalsIgnoreCase("boolean")) {
                if (questionList.elements.get(i).answer.trim().equalsIgnoreCase(questionList.elements.get(i).expected.trim())) {
                    report_route.add(questionList.elements.get(i).id);
                }
            } else if (questionList.elements.get(i).type.trim().equalsIgnoreCase("number")) {
                if (Double.valueOf(questionList.elements.get(i).bottom_interval) > Double.valueOf(questionList.elements.get(i).answer) ||
                        Double.valueOf(questionList.elements.get(i).top_interval) < Double.valueOf(questionList.elements.get(i).answer)) {
                    report_route.add(questionList.elements.get(i).id);
                }
            }
        }

        System.out.println("ROUTE:");
        List<String> route = new ArrayList<>();
        route.add(headID);
        for (int i = 0; i < questionList.elements.size(); i++) {
            route.add(questionList.get(i).id);
        }
        for (int i = 0; i <= route.size()-1; i++) {
            System.out.println(route.get(i));
        }

        System.out.println("TRUE ROUTE:");
        List<String> true_route = new ArrayList<>();
        String[] s = currentID.split("_");
        for (int i = 0; i < s.length; i++) {
            true_route.add(0, currentID);
            rollBackID();
        }
        for (int i = 0; i <= true_route.size()-1; i++) {
            System.out.println(true_route.get(i));
        }
        if (b) {
            report_list = true_route;
        } else {
            report_list = route;
        }

        System.out.println("REPORT ROUTE:");
        for (int i = 0; i <= report_list.size()-1; i++) {
            System.out.println(report_list.get(i));
        }

        /*state = State.REPORTING_START;
        Networking.post(Networking.query_link, SQL("insert", "reports", ));*/

    }

    public static void dismissAllDialogs(FragmentManager manager) {
        List<Fragment> fragments = manager.getFragments();

        if (fragments == null)
            return;

        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                DialogFragment dialogFragment = (DialogFragment) fragment;
                dialogFragment.dismissAllowingStateLoss();
            }

            try {
                FragmentManager childFragmentManager = fragment.getChildFragmentManager();
                if (childFragmentManager != null)
                    dismissAllDialogs(childFragmentManager);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        ReportActivity.questionList.elements.clear();
        ReportActivity.responseLists.clear();
        ReportActivity.questionNum = 0;
    }
}
