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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Report activity is the main functional activity. This handles the whole reporting process after logging in. **/
public class ReportActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, Callback {

    /** QR-code reader scanner. **/
    private ZXingScannerView scannerView;

    /** Elements in the view. **/
    Button scan;
    Button proceed;
    TextView textView;
    String text;

    /** Loading/process dialog.
     *  Showing when communicating with the server. **/
    ProgressDialog progressDialog;

    /** Json elements for the query. **/
    static JsonPrimitive username;
    static JsonPrimitive pass;
    static JsonPrimitive command;
    static JsonPrimitive table;
    static JsonPrimitive columns;
    static JsonPrimitive values;
    static JsonPrimitive selection;
    static JsonPrimitive specification;

    /** Json object which will contain the Json primitives above. **/
    static JsonObject jobj;

    /** List for the elements needed to be reported. **/
    List<String> report_list;

    /** Lists for questions and issues. **/
    static List<IssuesList> responseLists = new ArrayList<>();
    static QuestionsList questionList = new QuestionsList();

    /** All the id type needed to the process. **/
    private String machineID = "";
    private String machineType = "";
    private String reportID = "";
    static String currentID = "";
    private String headID = "";

    /** Selection of columns for different tables. **/
    private String issuesSelection = "id, cause, status";
    private String questionSelection = "id, question, question_type, expected, unit, interval_bottom, interval_top, leaf_solution";

    /** Number of the current question in the process. **/
    static int questionNum = 0;

    /** Enum for state type. **/
    public enum State {
        INITIAL,
        TYPE,
        HEADS,
        ISSUES,
        QUESTION,
        SUGGESTION,
        REPORTING_START,
        REPORTING_GETTING_ID,
        REPORTING
    }

    /** Current state of the process. **/
    public static State state;


    /** Setting initial text, setting view, initialize Network class and calling config function. **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        text = getResources().getString(R.string.report_info);
        config();
        Networking.initialize();
    }

    /** Configuring everything to the starting state. **/
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
                if (machineID.length()>0) {
                    nextStep();
                } else {
                    toast("Please scan the QR code of the machine before continuing");
                }
            }
        });
    }

    /** SQL request builder. Parameters are the type of query (select, insert etc.), the target table, the target columns for inserting,
     *  the inserted values, the selection of the columns from the table for selection, the specification of inserting and selecting
     *  (the part after WHERE in SQL command)
     *  Making a Json object which will be sent to the server to handle. **/
    public static String SQL(String mCommand, String mTable, String mColumns, String mValues, String mSelection, String mSpecification) {

        command = new JsonPrimitive(mCommand);
        table = new JsonPrimitive(mTable);
        columns = new JsonPrimitive(mColumns);
        values = new JsonPrimitive(mValues);
        selection = new JsonPrimitive(mSelection);
        specification = new JsonPrimitive(mSpecification);


        JsonObject object = new JsonObject();

        object.add("username", username);
        object.add("password", pass);
        object.add("command", command);
        object.add("table", table);
        object.add("columns", columns);
        object.add("values", values);
        object.add("selection", selection);
        object.add("specification", specification);

        jobj = object;
        System.out.println(jobj.toString());
        return object.toString();
    }

    /** Handling QR-code reader's result. Saving machine id. Changing text in the view. Saving machine id as a Json primitive for query. **/
    @Override
    public void handleResult(Result result) {
        machineID = result.getText();
        text = "'" + machineID + "' machine recognised. If that's correct please proceed with your report.";
        specification = new JsonPrimitive(result.getText());
        scannerView.stopCamera();
        setContentView(R.layout.activity_report);
        config();
    }

    /** Hiding process dialog and stopping camera, if the activity is not in focus. **/
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

    /** OkHttp's function. Called when the connection is not possible, server is unreachable. Hiding progress dialog. **/
    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Something went wrong!");
    }

    /** OkHttp's function. Called when the connection established and the server made a response. Hiding progress dialog.
     *  The server answer is in the response body. (basically what the server echoed back, calibrate on server side).
     *  We need to decode the response with UTF-8 charset and make a Json array out of it, because the string response
     *  is a json encoded response (set on server side). This way we can handle the response.
     *  Each state differs. We need different data from the response in each state. **/
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

        /** Adding id and cause fields from database response in the list of the issues. **/
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
        }
        /** Getting machine type. **/
        else if (state == State.TYPE) {
            if (Jarray != null) {
                try {
                    machineType = Jarray.getJSONObject(0).get("type").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        /** Getting the main issues, adding them to a list and calling displayDialog to pick type. Adding list as parameter. **/
        else if (state == State.HEADS) {
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
        }
        /** Getting the question's data for the issue. **/
        else if (state == State.QUESTION) {
            if (Jarray != null) {
                try {

                    questionList.add(Jarray.getJSONObject(0).get("id").toString(),
                            Jarray.getJSONObject(0).get("question_type").toString(),
                            Jarray.getJSONObject(0).get("question").toString(),
                            Jarray.getJSONObject(0).get("expected").toString(),
                            Jarray.getJSONObject(0).get("interval_top").toString(),
                            Jarray.getJSONObject(0).get("interval_bottom").toString(),
                            Jarray.getJSONObject(0).get("leaf_solution").toString(),
                            Jarray.getJSONObject(0).get("unit").toString());


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        /** Sending a request to get back the last record in the reports table and get the id from it. **/
        else if (state == State.REPORTING_START) {
            if (Jarray == null) {
                Networking.post(Networking.query_link, SQL("select", "reports", "", "", "id", "user ='" + getIntent().getStringExtra("username") + "' ORDER BY UNIX_TIMESTAMP(time) DESC"), this);
            }
        }
        /** Getting back the last record in the reports table and getting the id from it. **/
        else if (state == State.REPORTING_GETTING_ID) {
            if (Jarray != null) {
                try {
                    reportID = Jarray.getJSONObject(0).get("id").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


        nextStep();
    }

    /** Called after each response from server. Depending on the current state of the process, the next step differs. **/
    private void nextStep() {
        System.out.println(state);
        switch (state) {
            /** If we are in the initial state, we still need the type of the machine. Switching to type state. **/
            case INITIAL:
                try {
                    state = State.TYPE;
                    Networking.post(Networking.query_link, SQL("select", "machines", "", "", "type", "id = '" + machineID + "'"), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            /** If we are in the type state, we need the main issues to the machine type. Switching to heads state. **/
            case TYPE:
                try {
                    state = State.HEADS;
                    Networking.post(Networking.query_link, SQL("select", machineType + "_ISSUES", "", "", issuesSelection, "status = 'Active' AND " + generateSelectID()), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            /** Just switching to the next state. **/
            case HEADS:
                state = State.ISSUES;
                break;
            /** Just switching to the next state. Initializing questionNum. Calling getNextQuestion function. **/
            case ISSUES:
                state = State.QUESTION;
                questionNum = 1;
                getNextQuestion();
                break;
            /** Calling askQuestion function. **/
            case QUESTION:
                askQuestion();
                break;
            /** Calling showSuggestion function. **/
            case SUGGESTION:
                showSuggestion();
                break;
            /** Just switching to the next state. **/
            case REPORTING_START:
                state = State.REPORTING_GETTING_ID;
                break;
            /** Just switching to the next state. Calling nextStep again. **/
            case REPORTING_GETTING_ID:
                state = State.REPORTING;
                nextStep();
                break;
            /** If we have still some reportable events, we insert them to the database. If not, we call dismissAllDialogs function with
             *  a fragment manager to the current activity. **/
            case REPORTING:
                try {
                    if (report_list.size() > 0) {
                        Networking.post(Networking.query_link, SQL("insert", "report_events", "id, node_id", "'" + reportID + "'" + ", '" + report_list.get(0) + "'", "", ""), this);
                        report_list.remove(0);
                    }else{
                        ReportActivity.dismissAllDialogs(getSupportFragmentManager());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /** Called to show the suggestion on solving the issue. Calls displayDialog. **/
    private void showSuggestion() {
        QuestionElement question = questionList.get(questionNum - 1);
        displayDialog("Suggestion", "suggestion", question.solution, null, null);
    }

    /** Called after a boolean type dialog is shown and the user gave his/her answer. Decides the next move. **/
    public void answerProcessBoolean(boolean bool) {
        if (bool) {
            questionList.get(questionNum - 1).answer = questionList.get(questionNum - 1).expected;
            moveForward();
        } else {
            questionList.get(questionNum - 1).answer = "";
            moveSide();
        }
    }

    /** Called after a suggestion dialog is shown and the user answers if that suggestion solved his/her issue or not.
     *  Parameter is true if it has been solve, false if not. Decides the next move. **/
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

    /** Called after a number type dialog is shown and the user gave his/her answer. Decides the next move. **/
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

    /** Moving forward in the tree of issues. If the current question did not have a solution, we are moving on to the next level.
     *  If it does, we set the state to suggestion and calling next step, which handles the situation. **/
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

    /** Moving on the same level in the tree. Rolling back the id and increasing the question number. Getting the next question. **/
    private void moveSide() {
        rollBackID();
        questionNum++;
        getNextQuestion();
    }

    /** Moving back on the issue tree. Rolling back the id and getting the next question. **/
    private void moveBack() {
        rollBackID();
        getNextQuestion();
    }

    /** Rolling back the id. Splitting by the _ characters and getting the previous id. Depends on the database records. **/
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

    /** Getting the latest element from the questions' list and calling the displayDialog function for that question, which handles the display
     *  of the dialog for the question. **/
    private void askQuestion() {
        QuestionElement question = questionList.get(questionNum - 1);
        if (question.type.trim().equalsIgnoreCase("boolean")) {
            questionList.get(questionNum - 1).answer = "";
        }
        displayDialog("Question " + questionNum + ":", question.type, question.question, question.unit, null);
    }

    /** Generating the next node's id for the question table. Returning a regular expression on the id as a String, which is used
     *  in the SQL request. **/
    private String generateSelectID() {
        if (state == State.HEADS) {
            return "(id NOT REGEXP '[0-9]+[_]')";
        } else if (state == State.ISSUES) {
            return "(id REGEXP '" + currentID + "[_]')";
        } else {
            return null;
        }
    }

    /** Searching the next node in the issue tree. Getting it's position and making a request for the question of that issue.
     *  If couldn't find an issue with the current state (current id) calls the moveBack function to move back on the issue tree (rolling the id back).
     *  If the current id matches the head and couldn't find an other issue, the solver failed. Displays failure dialog. **/
    private void getNextQuestion() {
        int pos = getQuestionPos(getNextRegex());
        if (pos >= 0) {
            IssueElement element = responseLists.get(0).elements.get(pos);
            try {
                Networking.post(Networking.query_link, SQL("select", machineType + "_QUESTIONS", "", "", questionSelection, "id = '" + element.id + "'"), this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!currentID.equals(headID)) {
            moveBack();
        } else {
            displayDialog("Couldn't solve issue", "failed", getResources().getString(R.string.failed), null, null);
        }
    }

    /** Returning the next node's position in the list of issues. Using regular expression tools. Parameter is the regular
     *  expression to the search. **/
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

    /** Returning a regular ecpression to help witht he search of the next node in the tree of issues. **/
    private String getNextRegex() {
        return "\\A" + currentID + "[_][0-9]+\\z";
    }

    /** Called if the user selected the main issue from the given list on the dialog.
     *  Parameter is the chosen issues id. Storing the id as the current id and the head's id.
     *  Sending a new request to the server to get the issue tree for that main id. **/
    public void picked(String id) {
        currentID = id;
        headID = id;
        try {
            Networking.post(Networking.query_link, SQL("select", machineType + "_ISSUES", "", "", issuesSelection, "status = 'Active' AND " + generateSelectID()), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Displaying the correct dialog. Parameters are the dialogs name/title, type, the text on the dialog,
     *  the unit if needed (number dialog type), the list if needed (picking dialog type).
     *  Current types: number, boolean, pick. **/
    private void displayDialog(String name, String type, String text, String unit, List list) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment dialog = ReportDialog.newInstance(name, type, text, unit, list);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    /** Showing progress dialog, if it's not shown already.
     *  Currently not used.
     *  TODO: Call it at the right places. If the user clicks multiple times before switching views/dialogs, it can crash. Calling this can solve that problem.**/
    private void showDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    /** Hiding progress dialog, if it's shown already. **/
    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    /** Showing a toast message. Parameter is the message. **/
    private void toast(final String s) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** At the end of the reporting, this function is called to insert the data on the reporting events in the database.
     *  The parameter is true if the solution was found, false if not. **/
    public void report(boolean b) {
        /** route contains all the visited nodes. **/
        System.out.println("ROUTE:");
        List<String> route = new ArrayList<>();
        route.add(headID);
        for (int i = 0; i < questionList.elements.size(); i++) {
            route.add(questionList.get(i).id);
        }
        for (int i = 0; i <= route.size() - 1; i++) {
            System.out.println(route.get(i));
        }

        /** true_route contains all the node from the head to the solution node. **/
        System.out.println("TRUE ROUTE:");
        List<String> true_route = new ArrayList<>();
        String[] s = currentID.split("_");
        for (int i = 0; i < s.length; i++) {
            true_route.add(0, currentID);
            rollBackID();
        }
        for (int i = 0; i <= true_route.size() - 1; i++) {
            System.out.println(true_route.get(i));
        }

        /** If the solution was found, we report the true_route and the solvedStateText will be solved.
         *  If the solution wasn't found, we report the route and teh solvedStateText will be unsolved. **/
        String solvedStateText;
        if (b) {
            report_list = true_route;
            solvedStateText = "SOLVED";
        } else {
            report_list = route;
            solvedStateText = "UNSOLVED";
        }

        /** Printing out the reported route. Used for debugging. **/
        System.out.println("REPORT ROUTE:");
        for (int i = 0; i <= report_list.size() - 1; i++) {
            System.out.println(report_list.get(i));
        }

        /** Switching the state to reporting_start, which tells that the process is at the start of the reporting.
         *  Inserting in the reports table that we made a report with a given ending state (this is what solvedStateText is used for). **/
        state = State.REPORTING_START;
        try {
            Networking.post(Networking.query_link, SQL("insert", "reports", "user, status", "'" + getIntent().getStringExtra("username") + "', '" + solvedStateText + "'", "", ""), this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** This function is used for dismissing all the dialogs left after the reporting process.
     *  Parameter is activity's fragment manager.
     *  Calling each dialogs onDestroy function. Setting the questionNum to zero, clearing the questions' list and the issues' list.
     *  TODO: Solve the dismissing without a big try block.**/
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
