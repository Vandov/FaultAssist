package hu.bme.iit.faultassist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
        f.name=name;
        f.question=question;
        f.type=type;
        f.issues=list;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        if(type == "pick"){
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_pick, null);
            listView = (ListView) v.findViewById(R.id.issues_list_view);

            List<String> temp = new ArrayList<>();
            temp.add("Más");
            temp.add("lesz");
            temp.add("IIT");

            /*for (int i = 0; i<issues.get(0).size(); i++){
                temp.add(issues.get(0).get("cause"));
            }*/

            final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, temp);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    final String item = (String) parent.getItemAtPosition(position);
                    System.out.println(item);
                }

            });

            builder.setView(v).setTitle("Pick the reported issue");
        }else if(type == "boolean"){
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_boolean, null);
            builder.setView(inflater.inflate(R.layout.dialog_report_boolean, null));
        }else{
            View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_report_number, null);
            builder.setView(inflater.inflate(R.layout.dialog_report_number, null));
        }

        builder.setTitle(name);
        builder.setMessage(question);

        return builder.create();
    }
}