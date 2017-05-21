package hu.bme.iit.faultassist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Class which handles all the dialog types. **/
public class ReportDialog extends DialogFragment {

    /** Data which needed by one or more dialogs to work. **/
    String name;
    String text;
    String type;
    String unit;
    ListView listView;
    List<ReturnValues> issues;

    /** Function which creates a new dialog with the given parameters.
     *  Parameters are the name/title of the dialog, the type of the dialog, the unit of the answer (for number types),
     *  list of the main issues (for pick types). **/
    static ReportDialog newInstance(String name, String type, String text, String unit, List<ReturnValues> list) {
        ReportDialog f = new ReportDialog();
        f.name = name;
        f.text = text;
        f.type = type;
        f.issues = list;
        f.unit = unit;
        return f;
    }

    /** Building the views and the elements in the view depending on the type of the dialog. **/
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        /** Pick type which lists the main issues. **/
        if (type.trim().equalsIgnoreCase("pick")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_pick, null);
            builder.setView(v);
            listView = (ListView) v.findViewById(R.id.issues_list_view);

            List<String> temp = new ArrayList<>();

            for (int i = 0; i < issues.size(); i++) {
                temp.add(issues.get(i).get("cause"));
            }

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, temp);
            listView.setAdapter(adapter);

            /** Getting back the id of the picked element from the list. **/
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    ((ReportActivity) getActivity()).picked(issues.get(position).get("id"));
                }

            });

            builder.setView(v).setTitle("Pick the reported issue");
        }

        /** Boolean type which gives a question and a yes-no type of answer buttons. **/
        else if (type.trim().equalsIgnoreCase("boolean")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_boolean, null);
            builder.setView(v);
            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            Button no_btn = (Button) v.findViewById(R.id.no_btn_report_dialog);
            Button yes_btn = (Button) v.findViewById(R.id.yes_btn_report_dialog);

            text_view.setText(text);

            /** On button clicked, checks the expected answer and calls the report activity's answerProcessBoolean
             *  function with a parameter which depends on the answer and teh expected answer. **/
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum - 1).expected.trim().equalsIgnoreCase("no")) {
                        ((ReportActivity) getActivity()).answerProcessBoolean(true);
                    } else {
                        ((ReportActivity) getActivity()).answerProcessBoolean(false);
                    }
                }
            });

            /** On button clicked, checks the expected answer and calls the report activity's answerProcessBoolean
             *  function with a parameter which depends on the answer and teh expected answer. **/
            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum - 1).expected.trim().equalsIgnoreCase("yes")) {
                        ((ReportActivity) getActivity()).answerProcessBoolean(true);
                    } else {
                        ((ReportActivity) getActivity()).answerProcessBoolean(false);
                    }
                }
            });
        }

        /** Number type which gives a question and a edit field for numbers.
         *  Also it contains the unit type of the answer and displays it to be clear to the user. **/
        else if (type.trim().equalsIgnoreCase("number")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_number, null);
            builder.setView(v);

            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            TextView value_type_text_view = (TextView) v.findViewById(R.id.value_type__text_view_report_dialog);
            final EditText editText = (EditText) v.findViewById(R.id.number_input_report_dialog);
            Button ok_btn = (Button) v.findViewById(R.id.ok_btn_report_dialog);

            text_view.setText(text);
            value_type_text_view.setText(unit);

            /** If a valid answer is give, calls the report activity's answerProcessNumber with the answer as a parameter. **/
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(editText.getText().toString().length()>0){
                        ((ReportActivity) getActivity()).answerProcessNumber(editText.getText().toString());
                    }
                }
            });

        }

        /** Suggestion type which shows a suggestion for the solution to the user and expects the user to
         *  answer if the suggestion solved his/her issue or not. **/
        else if (type.trim().equalsIgnoreCase("suggestion")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_suggestion, null);
            builder.setView(v);

            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            Button no_btn = (Button) v.findViewById(R.id.no_btn_report_dialog);
            Button yes_btn = (Button) v.findViewById(R.id.yes_btn_report_dialog);

            text_view.setText(text);

            /** On button clicked, calls the report activity's solved function with a parameter depending on the response of the user. **/
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).solved(false);
                }
            });

            /** On button clicked, calls the report activity's solved function with a parameter depending on the response of the user. **/
            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).solved(true);
                }
            });
        }

        /** Solved type, which shows the issue which occurred, thanks the user and shows a close button. **/
        else if (type.trim().equalsIgnoreCase("solved")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_solved, null);
            builder.setView(v);

            Button close_btn = (Button) v.findViewById(R.id.close_btn_report_dialog);
            TextView textView = (TextView) v.findViewById(R.id.text_view_solved);
            textView.setText(text);

            /** On button clicked, calls the report activity's report function with true parameter (solved issue). **/
            close_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).report(true);
                }
            });
        }

        /** Failed type, which tells the user about the unsuccessful issue search, apologises the user, tells him/her to contact
         *  a technician and shows a close button. **/
        else if (type.trim().equalsIgnoreCase("failed")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_failed, null);
            builder.setView(v);

            Button close_btn = (Button) v.findViewById(R.id.close_btn_report_dialog);
            TextView textView = (TextView) v.findViewById(R.id.text_view_failed);
            textView.setText(text);

            /** On button clicked, calls the report activity's report function with false parameter (unsolved issue). **/
            close_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).report(false);
                }
            });
        }

        builder.setTitle(name);

        return builder.create();
    }

    /** Called when the dialog is closed. Depending on the type of the dialog and the state of the reporting process,
     *  switches back to the previous state, decreases the question number, sets the currentID in report activity or clears
     *  the lists used during the process. **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (type.trim().equalsIgnoreCase("boolean") || type.trim().equalsIgnoreCase("number")) {
            try {
                for (int i = 0; i < ReportActivity.responseLists.get(0).elements.size(); i++) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum - 1).id.equals(ReportActivity.responseLists.get(0).elements.get(i).id)) {
                        ReportActivity.responseLists.get(0).elements.get(i).visited = false;
                    }
                }
                ReportActivity.questionList.elements.get(ReportActivity.questionNum - 2).answer = "";
                ReportActivity.questionList.elements.remove(ReportActivity.questionNum - 1);

                ReportActivity.questionNum--;
                if (ReportActivity.questionNum <= 0) {
                    ReportActivity.state = ReportActivity.State.ISSUES;
                    ReportActivity.currentID = "";
                } else {
                    ReportActivity.currentID = ReportActivity.questionList.get(ReportActivity.questionNum - 1).id;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } else if (type.trim().equalsIgnoreCase("pick")) {

            try {
                ReportActivity.responseLists.get(0).elements.clear();
                ReportActivity.questionList.elements.clear();
            }catch (Exception e){
                e.printStackTrace();
            }
            ReportActivity.state = ReportActivity.State.INITIAL;
        } else if (type.trim().equalsIgnoreCase("suggestion")){
            ReportActivity.state = ReportActivity.State.QUESTION;
        } else if (type.trim().equalsIgnoreCase("solved")){
            ReportActivity.state = ReportActivity.State.INITIAL;
        }
    }
}