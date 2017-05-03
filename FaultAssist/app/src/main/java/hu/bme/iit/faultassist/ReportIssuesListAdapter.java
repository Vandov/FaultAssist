package hu.bme.iit.faultassist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ReportIssuesListAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public ReportIssuesListAdapter(Context context, String[] values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.report_rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.listLine);
        textView.setText(values[position]);
        return rowView;
    }
}
