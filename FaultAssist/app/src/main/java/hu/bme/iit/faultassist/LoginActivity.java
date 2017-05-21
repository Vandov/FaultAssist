package hu.bme.iit.faultassist;

import android.app.ProgressDialog;
import android.content.Intent;
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

/**This activity is the first activity in the process.
 * Allows the user to log in with username and password.**/
public class LoginActivity extends AppCompatActivity implements Callback {

    /** Elements in the view. **/
    EditText userID;
    EditText password;
    Button sign_in;
    Button register;

    /** Loading/process dialog.
     *  Showing when communicating with the server. **/
    ProgressDialog progressDialog;


    /** Setting the view, registering the view elements,setting the progress dialog,
     *  initializing the Network class elements, setting on click listeners. **/
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

        Networking.initialize();

        /** If sign in button clicked, adding the password and username to a Json object,
         *  which will be later sent to the server as a String.
         *  TODO: Switch to SQL function if it is moved to Network as a static method.
         *  To show that the activity is working, showing a progress dialog with a message.**/
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
                    Networking.post(Networking.login_link, json, LoginActivity.this);
                } catch (IOException e) {
                    hideDialog();
                    e.printStackTrace();
                }
            }
        });

        /** If register button pushed, stating the registration activity. **/
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }



    /** OkHttp's function. Called when the connection is not possible, server is unreachable. Hiding progress dialog. **/
    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Can't connect to server");
    }

    /** OkHttp's function. Called when the connection established and the server made a response. Hiding progress dialog.
     *  The server answer is in the response body. (basically what the server echoed back, calibrate on server side).
     *  If the answer contains Success, we start the report activity, and giving it the username and password. **/
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        hideDialog();
        String temp = response.body().string();
        toast(temp);

        if (temp.contains("Success")){
            Intent intent = new Intent(LoginActivity.this, ReportActivity.class);
            intent.putExtra("username", userID.getText().toString());
            intent.putExtra("password", password.getText().toString());
            startActivity(intent);
        }
    }

    /** Showing progress dialog, if it's not shown already. **/
    private void showDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    /** Hiding progress dialog, if it's shown already. **/
    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }


    /** Showing a toast message. Parameter is the message. **/
    private void toast(final String s){
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Making password an empty string, if the activity is not in focus. **/
    @Override
    protected void onPause(){
        super.onPause();
        try{
            password.setText("");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
