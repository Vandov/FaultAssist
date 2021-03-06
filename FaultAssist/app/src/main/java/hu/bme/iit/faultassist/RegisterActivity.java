package hu.bme.iit.faultassist;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity implements Callback {
    EditText userID;
    EditText password;
    EditText password_confirmation;
    Button register;
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userID = (EditText) findViewById(R.id.userID_registration);
        password = (EditText) findViewById(R.id.password_registration);
        password_confirmation = (EditText) findViewById(R.id.password_confirm_registration);
        register = (Button) findViewById(R.id.register_btn_registration);

        Networking.initialize();

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JsonObject object = new JsonObject();
                JsonPrimitive username = new JsonPrimitive(userID.getText().toString());
                JsonPrimitive pass = new JsonPrimitive(password.getText().toString());
                JsonPrimitive pass_conf = new JsonPrimitive(password_confirmation.getText().toString());

                object.add("username", username);
                object.add("password", pass);
                object.add("confirm-password", pass_conf);

                String json = object.toString();


                try {
                    progressDialog.setMessage("Registration in progress...");
                    showDialog();
                    Networking.post(Networking.register_link, json, RegisterActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Can't connect to server");
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        hideDialog();
        toast(response.body().string());
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

    @Override
    protected void onPause(){
        super.onPause();
        try{
            password.setText("");
            password_confirmation.setText("");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
