package hu.bme.iit.faultassist;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class Networking extends AsyncTask {
    private String login_link = "http://vm.ik.bme.hu:15513/mobile_login.php";
    private String register_link = "http://vm.ik.bme.hu:15513/mobile_register.php";
    private URL login_url;
    private URL register_url;

    private void config(){
        try {
            login_url = new URL(login_link);
            register_url = new URL(register_link);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void connect(String id, String pass){
        config();
        try {
            HttpURLConnection conn = (HttpURLConnection) login_url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("username", id));
            params.add(new BasicNameValuePair("password", pass));

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registration(String id, String pass, String pass_conf){
        config();

        try {
            HttpURLConnection conn = (HttpURLConnection) register_url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("username", id));
            params.add(new BasicNameValuePair("password", pass));
            params.add(new BasicNameValuePair("confirm-password", pass_conf));

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            if(params[0].toString()=="CONNECT"){
                connect(params[1].toString(), params[2].toString());
            }else if(params[0].toString()=="REGISTER"){
                registration(params[1].toString(), params[2].toString(), params[3].toString());
            }
        } catch (Exception e) {
            return e.toString();
        }
        return null;
    }
}
