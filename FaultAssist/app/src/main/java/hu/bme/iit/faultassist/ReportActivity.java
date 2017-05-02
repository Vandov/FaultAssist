package hu.bme.iit.faultassist;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.zxing.Result;
import java.io.IOException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        config();
        Networking.initialize();
    }

    private void config(){
        scan = (Button) findViewById(R.id.scan_btn_report);
        proceed = (Button) findViewById(R.id.proceed_btn_report);
        textView = (TextView) findViewById(R.id.text_view_report);

        textView.setText(text);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        username = new JsonPrimitive(getIntent().getStringExtra("username"));
        pass = new JsonPrimitive(getIntent().getStringExtra("password"));
        command = new JsonPrimitive("select");
        table = new JsonPrimitive("machines");
        values = new JsonPrimitive("");
        selection = new JsonPrimitive("type");
        specification = new JsonPrimitive("1");

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setMessage("Starting camera");
                progressDialog.show();
                scannerView = new ZXingScannerView(getApplicationContext());
                setContentView(scannerView);
                scannerView.setResultHandler(ReportActivity.this);
                progressDialog.hide();
                scannerView.startCamera();
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(text!=null){

                    JsonObject object = new JsonObject();

                    object.add("username", username);
                    object.add("password", pass);
                    object.add("command", command);
                    object.add("table", table);
                    object.add("values", values);
                    object.add("selection", selection);
                    object.add("specification", specification);

                    String json = object.toString();

                    try {
                        progressDialog.setMessage("Loading");
                        showDialog();
                        Networking.post(Networking.query_link, json, ReportActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    toast("Please scan the QR code of the machine before continuing");
                }
            }
        });
    }

    @Override
    public void handleResult(Result result) {
        text = "'" + result.getText().toString() + "' machine recognised. If that's correct please proceed with your report." ;
        specification = new JsonPrimitive(result.getText().toString());
        scannerView.stopCamera();
        setContentView(R.layout.activity_report);
        config();
    }

    @Override
    protected void onPause(){
        super.onPause();
        progressDialog.hide();
        try{
            scannerView.stopCamera();
        }catch (Exception e){
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
        String temp = response.body().string();
        if(!temp.isEmpty()){
            toast(temp);
            /*Intent intent = new Intent(ReportActivity.this, ReportEventActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);*/
        }
    }

    private void showDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }
    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void toast(final String s){
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
