package com.bitbeyond.tmall;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by luyufa on 2017/11/8.
 *
 */

public class ConfigActivity extends AppCompatActivity {

    private EditText account;
    private EditText passowrd;

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

        String s = getSharedPreferences("Account",MODE_PRIVATE).getString("user","");

        account = (EditText) findViewById(R.id.account);
        passowrd = (EditText) findViewById(R.id.password);

        if (!s.equals("")){
            account.setText(s);
            AlertDialog.Builder builde = new AlertDialog.Builder(this);
            builde.setTitle("提示");
            builde.setMessage("30秒后将自动开启任务，是否取消？");
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
            handler.sendMessageDelayed(message,30 * 1000);
        }

        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String user = account.getText().toString().trim();

                SharedPreferences sp = getSharedPreferences("Account",MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.putString("user",user);
                editor.commit();

                start(user);

            }
        });

    }

    private void start(String account){
        Intent intent = new Intent();
        intent.putExtra("account",account);
        intent.setClass(ConfigActivity.this,MainActivity.class);
        ConfigActivity.this.startActivity(intent);

    }
}
