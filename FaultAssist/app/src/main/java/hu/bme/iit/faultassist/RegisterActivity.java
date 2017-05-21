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

/** Activity for registration. Allows user to give a password, confirm the given password by typing it again and giving his/her name
 *  as the username. Server side searches for the username in a table (if found registration is allowed), checks if the passwords match,
 *  check if the user is not already registered, also checks the password length. Password min length is 8 (defined by mysql database).**/
public class RegisterActivity extends AppCompatActivity implements Callback {

    /** Elements in the view. **/
    EditText userID;
    EditText password;
    EditText password_confirmation;
    Button register;

    /** Loading/process dialog.
     *  Showing when communicating with the server. **/
    ProgressDialog progressDialog;

    /** Setting the view, registering the view elements,setting the progress dialog,
     *  initializing the Network class elements, setting on click listeners. **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userID = (EditText) findViewById(R.id.userID_registration);
        password = (EditText) findViewById(R.id.password_registration);
        password_confirmation = (EditText) findViewById(R.id.password_confirm_registration);
        register = (Button) findViewById(R.id.register_btn_registration);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading");

        Networking.initialize();

        /** If register button clicked, adding the password, password confirmation and username to a Json object,
         *  which will be later sent to the server as a String.
         *  TODO: Switch to SQL function if it is moved to Network as a static method.
         *  To show that the activity is working, showing a progress dialog with a message.**/
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

    /** OkHttp's function. Called when the connection is not possible, server is unreachable. Hiding progress dialog. **/
    @Override
    public void onFailure(Call call, IOException e) {
        hideDialog();
        toast("Can't connect to server");
    }

    /** OkHttp's function. Called when the connection established and the server made a response. Hiding progress dialog.
     *  The server answer is in the response body. (basically what the server echoed back, calibrate on server side).
     *  The response contains information about the process. **/
    @Override
    public void onResponse(Call call, Response response) throws IOException {
        hideDialog();
        toast(response.body().string());
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

    /** Making password and password confirmation an empty string, if the activity is not in focus. **/
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
