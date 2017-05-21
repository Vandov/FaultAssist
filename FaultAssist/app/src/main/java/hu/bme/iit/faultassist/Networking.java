package hu.bme.iit.faultassist;

import java.io.IOException;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


/** Basic network functionality. Storing all the links for register, login and query functions. All static,
 *  no object needed.
 *  TODO: move SQL function here. **/
public class Networking {
    public static String login_link = "http://vm.ik.bme.hu:20951/mobile_login.php";
    public static String register_link = "http://vm.ik.bme.hu:20951/mobile_register.php";
    public static String query_link = "http://vm.ik.bme.hu:20951/mobile_query.php";

    public static MediaType JSON;
    public static OkHttpClient client;

    /** Setting the basics for communication. Json type and UTF-8 charset **/
    public static void initialize() {
        client = new OkHttpClient();
        JSON = MediaType.parse("application/json; charset=utf-8");
    }

    /** Building the request and enqueuing it to the caller class / object.
     *  The caller class must implement Callback. **/
    public static void post(String url, String json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(Networking.JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Networking.client = new OkHttpClient();
        Networking.client.newCall(request).enqueue(callback);
    }
}
