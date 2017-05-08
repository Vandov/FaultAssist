package hu.bme.iit.faultassist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.DialogFragment;
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

            text_view.setText(question);
            no_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum-1).expected.trim().equalsIgnoreCase("nem")){
                        ((ReportActivity)getActivity()).answerProcessBoolean(true);
                    }else{
                        ((ReportActivity)getActivity()).answerProcessBoolean(false);
                    }
                }
            });

            yes_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ReportActivity.questionList.get(ReportActivity.questionNum-1).expected.trim().equalsIgnoreCase("igen")){
                        ((ReportActivity)getActivity()).answerProcessBoolean(true);
                    }else{
                        ((ReportActivity)getActivity()).answerProcessBoolean(false);
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