package hu.bme.iit.faultassist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity implements Callback {
    EditText userID;
    EditText password;
    Button sign_in;
    Button register;
    ProgressDialog progressDialog;

    MediaType JSON;
    OkHttpClient client;

    private String login_link = "http://vm.ik.bme.hu:20951/mobile_login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userID = (EditText)findViewById(R.id.userID_login);
        password = (EditText)findViewById(R.id.password_login);
        sign_in = (Button)findViewById(R.id.sign_in_btn_login);
        register = (Button)findViewById(R.id.register_btn_login);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        client = new OkHttpClient();
        JSON = MediaType.parse("application/json; charset=utf-8");


        sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JsonObject object = new JsonObject();
                JsonPrimitive username = new JsonPrimitive(userID.getText().toString());
                JsonPrimitive pass = new JsonPrimitive(password.getText().toString());

                object.add("username", username);
                object.add("password", pass);

                String json = object.toString();


                try {
                    progressDialog.setMessage("Logging you in...");
                    showDialog();
                    post(login_link, json);
                } catch (IOException e) {
                    hideDialog();
                    e.printStackTrace();
                }
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client = new OkHttpClient();
        client.newCall(request).enqueue(this);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Can't connect to server");
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        hideDialog();
        String temp = response.body().string();
        toast(temp);

        if (temp.contains("Success")){
            Intent intent = new Intent(LoginActivity.this, ReportActivity.class);
            startActivity(intent);
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
