package hu.bme.iit.faultassist;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.Result;

public class ReportActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView scannerView;
    Button scan;
    TextView textView;
    String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        config();
    }

    private void config(){
        scan = (Button) findViewById(R.id.scan_btn_report);
        textView = (TextView) findViewById(R.id.text_view_report);

        textView.setText(text);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerView = new ZXingScannerView(getApplicationContext());
                setContentView(scannerView);
                scannerView.setResultHandler(ReportActivity.this);
                scannerView.startCamera();
            }
        });
    }

    @Override
    public void handleResult(Result result) {
        text = result.getText().toString();
        scannerView.stopCamera();
        setContentView(R.layout.activity_report);
        config();
    }

    @Override
    protected void onPause(){
        super.onPause();
        try{
            scannerView.stopCamera();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
