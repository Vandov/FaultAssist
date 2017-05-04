package hu.bme.iit.faultassist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportDialog extends DialogFragment {

    String name;
    String question;
    String type;
    ListView listView;
    List<ReturnValues> issues;


    static ReportDialog newInstance(String name, String type, String question, List<ReturnValues> list) {
        ReportDialog f = new ReportDialog();
        f.name = name;
        f.question = question;
        f.type = type;
        f.issues = list;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        if (type == "pick") {
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
                    final String item = (String) parent.getItemAtPosition(position);


                    try {
                        ReportActivity.updateFields(ReportActivity.issues);
                        String spec;


                        spec = "status = 'Active' AND (id NOT REGEXP '[0-9]+[_][0-9]+[_]') AND (id REGEXP '[0-9]+[_]') ORDER BY id";

                        String json = ReportActivity.SQL("select", ReportActivity.machineType + "_ISSUES", "", "id, cause, status", spec);
                        Networking.post(Networking.query_link, json, (ReportActivity) getActivity());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });

            builder.setView(v).setTitle("Pick the reported issue");
        } else if (type.trim().equalsIgnoreCase("boolean")) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_boolean, null);
            builder.setView(v);
            TextView text_view = (TextView) v.findViewById(R.id.text_view_report);
            Button no_btn = (Button) v.findViewById(R.id.no_btn_report_dialog);
            Button yes_btn = (Button) v.findViewById(R.id.yes_btn_report_dialog);

            text_view.setText(question);
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        String spec;
                        spec = "status = 'Active' AND (id NOT REGEXP '[0-9]+[_][0-9]+[_]') AND (id REGEXP '[0-9]+[_]') ORDER BY id LIMIT " + ReportActivity.question_helper_num + "-1,1";
                        String json = ReportActivity.SQL("select", ReportActivity.machineType + "_ISSUES", "", "id, cause, status", spec);
                        Networking.post(Networking.query_link, json, (ReportActivity) getActivity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        ReportActivity.question_helper_num++;
                        System.out.println(ReportActivity.question_helper_num);
                        String spec;
                        spec = "status = 'Active' AND (id NOT REGEXP '[0-9]+[_][0-9]+[_]') AND (id REGEXP '[0-9]+[_]') ORDER BY id LIMIT " + ReportActivity.question_helper_num + "-1,1";
                        System.out.println(spec);
                        String json = ReportActivity.SQL("select", ReportActivity.machineType + "_ISSUES", "", "id, cause, status", spec);
                        Networking.post(Networking.query_link, json, (ReportActivity) getActivity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_number, null);
            builder.setView(inflater.inflate(R.layout.dialog_report_number, null));
        }

        builder.setTitle(name);
        //builder.setMessage(question);

        return builder.create();
    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();
        ReportActivity.questionNum-=3;
        ReportActivity.requestNum-=3;
        ReportActivity.resendLastRequest((ReportActivity)getActivity());
    }*/
}