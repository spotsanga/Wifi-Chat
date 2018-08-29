package rsi.wifichat;

import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.spec.ECField;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Button b2 = (Button) findViewById(R.id.b2);
        if (b2 != null) {
            b2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setContentView(R.layout.client);
                    final Client cl = new Client();
                    cl.start_client();
                    Button b3 = (Button) findViewById(R.id.clientsend);
                    b3.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cl.send_msg();
                        }
                    });
                }
            });
        }

        Button b1 = (Button) findViewById(R.id.b1);
        if (b1 != null) {
            b1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setContentView(R.layout.host);
                    final Server sr = new Server();
                    sr.start_server();
                    Button b4 = (Button) findViewById(R.id.hostsend);
                    b4.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sr.send_msg();
                        }
                    });
                }
            });
        }
    }

    class Server{
        Socket cls[]=new Socket[100];
        int n=0;
        ServerSocket ss=null;
        LinearLayout ll=(LinearLayout)findViewById(R.id.hl);
        ScrollView sv=(ScrollView)findViewById(R.id.hsv);
        void start_server(){
            try {
                ss=new ServerSocket(3568);
                Thread th=new Thread(){
                    public void run(){
                        while(true){
                            try {
                                final Socket s=ss.accept();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView tv = new TextView(getApplicationContext());
                                        tv.setText(String.format("New Client Joined: "+s));
                                        ll.addView(tv);
                                        tv = new TextView(getApplicationContext());
                                        tv.setText("");
                                        ll.addView(tv);
                                        sv.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                                cls[n]=s;
                                n++;
                                Thread th1=new Thread(){
                                    public void run(){
                                        while(true){
                                            try {
                                                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                                final String txt = inFromClient.readLine();
                                                if(txt==null||txt.isEmpty()||txt.equals(""))
                                                    break;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        TextView tv = new TextView(getApplicationContext());
                                                        tv.setBackgroundColor(Color.RED);
                                                        tv.setText(txt);
                                                        ll.addView(tv);
                                                        tv = new TextView(getApplicationContext());
                                                        tv.setText("");
                                                        ll.addView(tv);
                                                        sv.fullScroll(View.FOCUS_DOWN);
                                                    }
                                                });
                                                for(int i=0;i<n;i++) {
                                                    if(cls[i]==s||cls[i]==null)
                                                        continue;
                                                    try {
                                                        DataOutputStream outToServer = new DataOutputStream(cls[i].getOutputStream());
                                                        outToServer.writeBytes(txt + '\n');
                                                        Log.v("Server send:(recived)","success"+txt);
                                                    }
                                                    catch(IOException e){
                                                        Log.v("Server send(recieved):", String.format("failed" + i));
                                                        cls[i].close();
                                                        cls[i]=null;
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            catch(IOException e){
                                                try {
                                                    Log.v("Server recieve:", String.format("failed"+s));
                                                    s.close();
                                                } catch (IOException e1) {
                                                    e1.printStackTrace();
                                                }
                                                e.printStackTrace();
                                                break;
                                            }
                                        }
                                    }
                                };
                                th1.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                };
                th.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        void send_msg(){
            EditText et = (EditText) findViewById(R.id.hostbox);
            String txt = null;
            if (et != null) {
                txt = et.getText().toString();
            }
            if(txt!=null&&!txt.isEmpty()) {
                TextView tv = new TextView(getApplicationContext());
                tv.setGravity(Gravity.END);
                tv.setBackgroundColor(Color.BLUE);
                tv.setText(txt);
                ll.addView(tv);
                tv = new TextView(getApplicationContext());
                tv.setText("");
                ll.addView(tv);
                sv.fullScroll(View.FOCUS_DOWN);
                for (int i = 0; i < n; i++) {
                    if (cls[i] == null)
                        continue;
                    try {
                        DataOutputStream outToServer = new DataOutputStream(cls[i].getOutputStream());
                        String name= "("+Build.MODEL+")-";
                        outToServer.writeBytes(name+txt + '\n');
                        Log.v("Server send:", "success"+txt);
                    } catch (IOException e) {
                        try {
                            cls[i].close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        Log.v("Server send:", String.format("failed" + i));
                        cls[i] = null;
                        e.printStackTrace();
                    }
                }
                et.setText("");
            }
        }
    }

    class Client {
        ScrollView sv=(ScrollView)findViewById(R.id.csv);
        Socket s=null;
        String txt;
        LinearLayout ll=(LinearLayout)findViewById(R.id.ll);
        void start_client() {
            try {
                TextView tv = new TextView(getApplicationContext());
                tv.setGravity(Gravity.CENTER);
                tv.setText("Searching Server");
                ll.addView(tv);
                s = new Socket("192.168.43.1", 3568);
                tv = new TextView(getApplicationContext());
                tv.setGravity(Gravity.CENTER);
                tv.setText("Connected to Server");
                ll.addView(tv);
                Thread th=new Thread(){
                    public void run(){
                        while(true){
                            try {
                                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                txt = inFromClient.readLine();
                                if(txt.equals(""))
                                    break;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        TextView tv = new TextView(getApplicationContext());
                                        tv.setBackgroundColor(Color.RED);
                                        tv.setText(txt);
                                        ll.addView(tv);
                                        tv = new TextView(getApplicationContext());
                                        tv.setText("");
                                        ll.addView(tv);
                                        sv.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                            }
                            catch(IOException e){
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                };
                th.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        void send_msg() {
            EditText et = (EditText) findViewById(R.id.clientbox);
            String txt = et.getText().toString();
            et.setText("");
            if(!txt.equals("")) {
                try {
                    TextView tv = new TextView(getApplicationContext());
                    tv.setGravity(Gravity.RIGHT);
                    tv.setBackgroundColor(Color.BLUE);
                    tv.setText(txt);
                    ll.addView(tv);
                    tv = new TextView(getApplicationContext());
                    tv.setText("");
                    ll.addView(tv);
                    sv.fullScroll(View.FOCUS_DOWN);
                    DataOutputStream outToServer = new DataOutputStream(s.getOutputStream());
                    String name= "("+Build.MODEL+")-";
                    outToServer.writeBytes(name+txt + '\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

