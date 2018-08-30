package com.example.screeningapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.MainThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import javax.security.auth.login.LoginException;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "LoginActivity";
    private EditText usernameText;
    private EditText passwordText;
    private String username = "";//用户名
    private String password;//wifi密码
    private SocketService.SendMsgBinder mBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (SocketService.SendMsgBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private int chairManFlag = 2;//0=非主席，1=主席
    private int sameNameFlag = 2;//0=不重名，1=重名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LogUtil.level = 6;

        usernameText = (EditText) findViewById(R.id.username_edittext);
        passwordText = (EditText) findViewById(R.id.password_edittext);
        password = passwordText.getText().toString();
        Button login = (Button) findViewById(R.id.login_btn);
        login.setOnClickListener(LoginActivity.this);

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.screeningapplication.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,intentFilter);

        Log.e(TAG, "onCreate: master" );
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.login_btn:
                username = usernameText.getText().toString();
                if (!checkInput()){
                    Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent bindIntent = new Intent(LoginActivity.this, SocketService.class);
                bindService(bindIntent, connection, BIND_AUTO_CREATE);
                bindIntent.putExtra("username", username);
                startService(bindIntent);
                break;
            default:
                break;
        }
    }

    /*
    * 检查输入是否合法
    * */
    private boolean checkInput(){
        if (username.isEmpty()){
            return false;
        }
        return true;
    }

    /*
    * 本地广播接收器，
    * 接收用户信息ArrayList
    * */
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MyApplication.getContext(), "收到广播了", Toast.LENGTH_SHORT).show();
            int category = intent.getIntExtra("category", -1);
            switch (category){
                //主席信号
                case Category.CHAIRMAN:
                    chairManFlag = 1;
                    if (sameNameFlag == 0){ //是主席，不重名，Activity跳转
                        LogUtil.i("LocalReceiver", "是主席，不重名，Activity跳转");
                        ChairmanActivity.actionStart(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(sameNameFlag == 1){ //是主席，重名了，Activity不跳转
                        Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                //非主席信号
                case Category.NON_CHAIRMAN:
                    chairManFlag = 0;
                    if (sameNameFlag == 0){ //非主席，不重名，Activity跳转
                        LogUtil.i("LocalReceiver", "非主席，不重名，Activity跳转");
                        NonchairmanActivity.startActivity(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(sameNameFlag == 1){ //非主席，重名了，Activity不跳转
                        Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                //重名信号
                case Category.SAME_NAME:
                    sameNameFlag = 0;
                    Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                            .show();
                    break;
                //不重名信号
                case Category.DIFFERENT_NAME:
                    sameNameFlag = 1;
                    if (chairManFlag == 0){ //不重名，非主席，Activity跳转
                        LogUtil.i("LocalReceiver", "不重名，非主席，Activity跳转");
                        NonchairmanActivity.startActivity(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(chairManFlag == 1){ //不重名，是主席，Activity跳转
                        LogUtil.i("LocalReceiver", "不重名，是主席，Activity跳转");
                        ChairmanActivity.actionStart(LoginActivity.this);
                        unbindService(connection);
//                        LoginActivity.this.finish();
                    }
                    break;
                case Category.FAILED_TO_CONNECT:
                    LogUtil.i("LocalReceiver", "socket连接失败");
                    Toast.makeText(LoginActivity.this, "socket连接失败", Toast.LENGTH_SHORT)
                            .show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtil.i("onSaveInstanceState", "");
        Log.i(TAG, "onSaveInstanceState: " );
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        super.onDestroy();
        LogUtil.i("onDestroy","登录界面被销毁了");
    }
}
