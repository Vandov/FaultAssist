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
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ReportDialog extends DialogFragment {

    String name;
    String text;
    String type;
    ListView listView;
    List<ReturnValues> issues;


    static ReportDialog newInstance(String name, String type, String text, List<ReturnValues> list) {
        ReportDialog f = new ReportDialog();
        f.name = name;
        f.text = text;
        f.type = type;
        f.issues = list;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

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

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    ((ReportActivity) getActivity()).picked(issues.get(position).get("id"));
                }

            });

            builder.setView(v).setTitle("Pick the reported issue");
        } else if (type.trim().equalsIgnoreCase("boolean")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_boolean, null);
            builder.setView(v);
            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            Button no_btn = (Button) v.findViewById(R.id.no_btn_report_dialog);
            Button yes_btn = (Button) v.findViewById(R.id.yes_btn_report_dialog);

            text_view.setText(text);
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum - 1).expected.trim().equalsIgnoreCase("nem")) {
                        ((ReportActivity) getActivity()).answerProcessBoolean(true);
                    } else {
                        ((ReportActivity) getActivity()).answerProcessBoolean(false);
                    }
                }
            });

            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum - 1).expected.trim().equalsIgnoreCase("igen")) {
                        ((ReportActivity) getActivity()).answerProcessBoolean(true);
                    } else {
                        ((ReportActivity) getActivity()).answerProcessBoolean(false);
                    }
                }
            });
        } else if (type.trim().equalsIgnoreCase("number")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_number, null);
            builder.setView(v);
        } else if (type.trim().equalsIgnoreCase("suggestion")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_suggestion, null);
            builder.setView(v);

            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            Button no_btn = (Button) v.findViewById(R.id.no_btn_report_dialog);
            Button yes_btn = (Button) v.findViewById(R.id.yes_btn_report_dialog);

            text_view.setText(text);
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).solved(false);
                }
            });

            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ReportActivity) getActivity()).solved(true);
                }
            });
        } else if (type.trim().equalsIgnoreCase("solved")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_solved, null);
            builder.setView(v);
            Button close_btn = (Button) v.findViewById(R.id.close_btn_report_dialog);
            TextView textView = (TextView) v.findViewById(R.id.text_view_solved);
            textView.setText(text);

            close_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /************************************************************************************CALL REPORT HERE*****************/
                    ReportActivity.dismissAllDialogs(getFragmentManager());
                }
            });
        }

        builder.setTitle(name);

        return builder.create();
    }

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
        }
    }
}