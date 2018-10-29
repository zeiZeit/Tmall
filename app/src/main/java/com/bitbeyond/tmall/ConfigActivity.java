package com.bitbeyond.tmall;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by luyufa on 2017/11/8.
 *
 */

public class ConfigActivity extends AppCompatActivity {

    private EditText account;
    private EditText passowrd;
    private TextView version;

    AlertDialog.Builder builde ;
    private static final int START = 0x101;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:{
                    String user = (String) msg.obj;

                    start(user);

                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);


        account = (EditText) findViewById(R.id.account);
        passowrd = (EditText) findViewById(R.id.password);
        Button submit = (Button) findViewById(R.id.submit);
        version = (TextView) findViewById(R.id.version);
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = getPackageManager().
                    getPackageInfo(getPackageName(), 0).versionCode;
            Log.i("main", "versionCode="+versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        version.setText("版本号为："+versionCode);


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences("Account",MODE_PRIVATE);
                String pass = passowrd.getText().toString().trim();
                String newUsers = "";
                if (pass.equals("x")){//x 从存储的账号中删除输入的账号
                    String user = account.getText().toString().trim();


                    String users = sp.getString("users",null);
                    int j =10;

                    if (users!=null){
                        String[] arrUsers = users.split("_");
                        for (int i=0;i<arrUsers.length;i++){
                            if (arrUsers[i].equals(user)){
                                j = i;
                                break;
                            }
                        }

                        for (int i=0;i<arrUsers.length;i++){
                            if (i!=j){
                                newUsers =newUsers+"_"+ arrUsers[i];
                            }

                        }
                        newUsers = newUsers.substring(1);
                        sp.edit().putString("users",newUsers).commit();

                    }

                }else {
                    String users = sp.getString("users",null);
                    String user = account.getText().toString().trim();

                    SharedPreferences.Editor editor = sp.edit();
                    if (users==null||users.equals("")){
                        editor.putString("users",user).commit();
                    }else if (!users.contains(user)){
                        users = users+"_"+user;
                        editor.putString("users",users).commit();
                    }
                    start(user);
                }

            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        String users = getSharedPreferences("Account",MODE_PRIVATE).getString("users",null);
        Log.i("main", "users ="+users);
        String s = "";

        if (!(users==null||users.equals(""))){
            if (users.contains("_")){
                int i = users.indexOf("_");
                s = users.substring(0,i);
                String s1 = users.substring(i+1);
                users = s1+"_"+s;
                getSharedPreferences("Account",MODE_PRIVATE).edit().putString("users",users).commit();
            }else {
                s = users;
            }

        }

        if (!s.equals("")){
            account.setText(s);
            builde = new AlertDialog.Builder(this);
            builde.setTitle("提示");
            builde.setMessage("10秒后将自动开启任务，是否取消？");
            builde.setNegativeButton("取消任务", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.removeCallbacksAndMessages(null);
                }
            });
            builde.create().show();

            Message message = new Message();
            message.what = START;
            message.obj = s;
            handler.sendMessageDelayed(message,10 * 1000);
        }


    }

    private void start(String account){
        Intent intent = new Intent();
        intent.putExtra("account",account);
        intent.setClass(ConfigActivity.this,MainActivity.class);
        ConfigActivity.this.startActivity(intent);

    }
}
