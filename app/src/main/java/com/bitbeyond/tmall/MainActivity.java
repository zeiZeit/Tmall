package com.bitbeyond.tmall;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import redis.clients.jedis.Jedis;

public class MainActivity extends AppCompatActivity {

    private static String loginUrl = "https://login.taobao.com/member/login.jhtml?from=taobao&full_redirect=true&style=minisimple&minititle=&minipara=0,0,0&sub=true";

    private static final String LoginTop = "javascript:" +
            "document.getElementById('TPL_username_1').value = '";

    private static final String LoginBottom = "';\n" +
            "document.getElementById('TPL_password_1').value = '12QWaszx';\n" +
            "document.getElementById('TPL_password_1').focus();\n" +
            "document.getElementById('J_SubmitStatic').click();";
    private String account = "";


    private static final String InjectionCookies = "javascript:document.cookie";
    private static final String Injection = "javascript:document.body.innerText";
    private static final String GetDataId = "javascript:var nodes = document.getElementsByTagName('dl'); var array=[];for(var i =0; i < document.getElementsByTagName('dl').length; i++){array.push(nodes[i].getAttribute('data-id'));}array";

    private String SlideNexus4 = "input swipe 436 335 640 334 800\n";
    private String TapNexus4 = "input tap 544 334\n";

    private String SlideNexus5 = "input swipe 617 486 905 486 800\n";
    private String TapNexus5 = "input tap 762 484\n";

    private String taskUrl = "";
    private String taskId = "";
    private String result = "";
    private long acceptTime = 0l;

    private int index = 0;
    private int error_count = 0;

    private byte[] results;

    private String TAG = getClass().getSimpleName();
    private RootShell shell;

    private Jedis jedis;
    private boolean shouldLoad = true;

    private ViewGroup mViewParent;
    private WebView mWebView;

    private String mSignKey = "";
    private long mEnqueue;
    private DownLoadReceiver mDownLoadReceiver;
    private int mSuccess = 0;

    private static final int AUTH = 0x101;
    private static final int LOAD = 0x102;
    private static final int GET = 0x103;
    private static final int INJECTION = 0x104;
    private static final int PUSH = 0x105;
    private static final int CHECK = 0x106;
    private static final int INPUT = 0x107;
    private static final int OPEN = 0x108;
    private static final int CLOSE = 0x109;
    private static final int injection = 0x110;//??????
    private static final int REBOOT = 0x111;//重启
    private static final int SIGN = 0x112;//签到
    private static final int UPDATE = 0x113;//更新查询
    private static final int DOWN = 0x114;//下载成功
    private static final int SHOW = 0x115;//开始下载
    private static final int RESTART = 0x116;//开始下载


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case AUTH: {
                    new AuthTask().execute();
                    break;
                }
                case LOAD: {
                    mWebView.loadUrl(taskUrl);
                    break;
                }
                case GET: {
                    removeMessages(GET);
                    new GetTask().execute();
                    break;
                }
                case INJECTION: {
                    mWebView.evaluateJavascript(LoginTop + account + LoginBottom, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            Log.i(TAG, "登录时注入结果：     " + s);
                        }
                    });

                    //mHandler.sendEmptyMessageDelayed(RESTART,30*60*1000);
                    break;
                }
                case PUSH: {
                    new PushTask().execute();
                    break;
                }
                case CHECK: {

                    String url = (String) msg.obj;
                    mWebView.loadUrl(url);
                    break;
                }
                case INPUT: {
                    String label = (String) msg.obj;
                    Log.i(TAG, "填入验证码");
                    mWebView.evaluateJavascript("document.getElementById('checkcodeInput').value = '" + label + "';", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            //填入验证码
                            Log.i(TAG, s);

                            Log.i(TAG, "提交验证码");
                            mWebView.evaluateJavascript("document.evaluate('//div[@class=\"submit\"]/input', document).iterateNext().click();", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {
                                    //提交
                                }
                            });
                        }
                    });

                    Log.i(TAG, "结束计时" + System.currentTimeMillis());
                    break;
                }
                case OPEN: {
                    Log.i(TAG, "打开移动网络");

                    MobileSwitch.open(MainActivity.this);
                    Toast.makeText(MainActivity.this, "正在打开移动网络，30秒后开始任务", Toast.LENGTH_LONG).show();

                    Message message = new Message();
                    message.obj = loginUrl;
                    message.what = LOAD;
                    mHandler.sendMessageDelayed(message, 1000 * 30);
                    break;
                }
                case CLOSE: {
                    Log.i(TAG, "关闭移动网络");

                    shell.execute("reboot\n");

                    break;
                }
                case injection: {
                    injection();
                    break;
                }
                case REBOOT: {
                    mHandler.removeMessages(REBOOT);
                    new RebootTask().execute();
                    mHandler.sendEmptyMessageDelayed(REBOOT, 5 * 60 * 1000);
                    break;
                }
                case SIGN: {
                    mHandler.removeMessages(SIGN);
                    new SignTask().execute();
                    mHandler.sendEmptyMessageDelayed(SIGN, 5 * 60 * 1000);
                    break;
                }
                case UPDATE: {
                    mHandler.removeMessages(UPDATE);
                    new UpdateTask().execute();
                    mHandler.sendEmptyMessageDelayed(UPDATE, 5 * 60 * 1000);
                    break;
                }
                case SHOW: {
                    Toast.makeText(MainActivity.this, "Down success", Toast.LENGTH_SHORT).show();
                    break;
                }
                case DOWN: {
                    Toast.makeText(MainActivity.this, "downloading", Toast.LENGTH_SHORT).show();
                    break;
                }
                case RESTART:
                    removeMessages(GET);
                    removeMessages(GET);
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    String imei;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
        } catch (Exception e) {
            imei = "";
        }

        Intent intent = getIntent();
        try {
            account = intent.getStringExtra("account");
        } catch (Exception e) {
            e.printStackTrace();
        }

        shell = RootShell.open();

        mDownLoadReceiver = new DownLoadReceiver();
        mViewParent = (ViewGroup) findViewById(R.id.webView1);
        init(mViewParent);
    }

    private void init(ViewGroup viewGroup) {

        mWebView = new WebView(this, null);
        viewGroup.addView(mWebView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mWebView.setWebViewClient(webViewClient);
        WebSettings webSetting = mWebView.getSettings();
        webSetting.setUseWideViewPort(true);//让webview读取网页设置的viewport，pc版网页
        webSetting.setLoadWithOverviewMode(true);
        webSetting.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSetting.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSetting.setDisplayZoomControls(true); //隐藏原生的缩放控件
        webSetting.setJavaScriptEnabled(true);
        webSetting.setUserAgentString("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        webSetting.setDefaultTextEncodingName("utf-8");
        webSetting.setDomStorageEnabled(true);
        webSetting.setAllowFileAccessFromFileURLs(true);
        webSetting.setBlockNetworkImage(true);//阻塞图片加载线程

        webSetting.setLoadsImagesAutomatically(false);//不允许加载图片

        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "Tmall_zz");
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();

        mWebView.loadUrl(loginUrl);
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            // 在这里处理html源码
            Log.i(TAG, "html:\n"+html);
            result = html;
            mHandler.sendEmptyMessage(PUSH);
        }
    }


    WebViewClient webViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return shouldOverrideUrlLoadingByApp(url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG, "onPageStarted ----------    " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.i(TAG, "onPageFinished      " + url);

            mWebView.evaluateJavascript(InjectionCookies, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Log.w(TAG, s);
                    if (s.length()>10)
                        new PushCookie().execute(s);
                }
            });

            if (url.contains("https://i.taobao.com/my_taobao.htm") || url.contains("https://www.taobao.com/")) {
                error_count = 0;

                if (shouldLoad) {
                    Log.i(TAG, "进入到我的淘宝页面，开始任务");
                    //进入到我的淘宝页面，开始任务
                    mHandler.sendEmptyMessageDelayed(AUTH, 100);
                    mHandler.sendEmptyMessage(SIGN);
                    mHandler.sendEmptyMessage(REBOOT);

                } else {
                    Log.i(TAG, "不是第一次进入我的淘宝页面，不取任务");
                }
            } else if (url.startsWith("https://login.taobao.com")) {
                error_count = 0;
                Log.i(TAG, "在登录界面");
                mHandler.sendEmptyMessageDelayed(INJECTION, 10 * 1000);

            } else if (url.startsWith("https://login.tmall.com")) {

                error_count = 0;

                CookieSyncManager.createInstance(MainActivity.this).resetSync();

                mWebView.loadUrl(loginUrl);

            } else if (url.contains("https://list.tmall.com/m/search_items.htm")) {

                error_count = 0;

                mWebView.evaluateJavascript(Injection, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.i(TAG, "搜索结果：     " + s);
                        result = s;
                        Log.i(TAG, "手机牌子为："+Build.MODEL);
                        if (s.contains("请按住滑块，拖动到最右边")){
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!Build.MODEL.equals("Nexus 5")) {

                                        shell.execute("input swipe 436 335 640 334 800\n");
                                        sleep(3);
                                        shell.execute("input tap 762 484\n");
                                        sleep(2);
                                        shell.execute("input swipe 430 333 646 334 1000\n");
                                        sleep(3);
                                        shell.execute("input tap 544 334\n");
                                        sleep(1);
                                        shell.execute("input swipe 432 333 645 334 1500\n");
                                        sleep(3);
                                        shell.execute("input tap 544 334\n");
                                        for (int i = 0; i < 7; i++) {
                                            sleep(1);
                                            shell.execute("input swipe 430 333 643 334 1800\n");
                                            sleep(3);
                                            shell.execute("input tap 544 334\n");
                                        }
                                        sleep(1);
                                        shell.execute("input swipe 431 333 643 333 1000\n");
                                        sleep(2);
                                        shell.execute("input tap 544 334\n");
                                    }else {
                                        shell.execute(SlideNexus5);
                                        sleep(3);
                                        shell.execute(TapNexus5);
                                        sleep(2);
                                        shell.execute("input swipe 615 486 903 487 1000\n");
                                        sleep(3);
                                        shell.execute(TapNexus5);
                                        sleep(1);
                                        shell.execute("input swipe 613 485 909 487 1500\n");
                                        sleep(3);
                                        shell.execute(TapNexus5);
                                        for (int i = 0; i < 7; i++) {
                                            sleep(1);
                                            shell.execute(SlideNexus5);
                                            sleep(3);
                                            shell.execute(TapNexus5);
                                        }
                                        sleep(1);
                                        shell.execute("input swipe 616 485 902 487 1000\n");
                                        sleep(2);
                                        shell.execute(TapNexus5);
                                    }
                                    mHandler.sendEmptyMessageDelayed(GET,3000);
                                }
                            }).start();

                        }else {
                            mHandler.sendEmptyMessage(PUSH);
                        }

                    }
                });
                //https://h5api.m.taobao.com/h5/mtop.taobao.detail
            } else if (url.startsWith("https://sec.taobao.com/query.htm")) {

                //这是验证码界面，统计一下打验证码次数
                error_count = error_count + 1;
                if (error_count > 5) {
                    shell.execute("reboot\t");
                }

                Log.i(TAG, "开始计时" + System.currentTimeMillis());

                mWebView.evaluateJavascript("document.getElementById('checkcodeImg').src", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.i(TAG, s);
                        s = s.substring(1, s.length() - 1);
                        imageByte(s);

                    }
                });
            } else if (url.contains("http://err.tmall.com/error2.html") || url.contains("http://err.tmall.com/error1.html")) {

                error_count = 0;

                result = "No such shop";
                mHandler.sendEmptyMessage(PUSH);

            } else if (url.contains("http://api.m.taobao.com/h5/mtop.taobao") || url.contains("https://h5api.m.taobao.com/h5/mtop.taobao.detail") ||
                    url.contains("taobao.com/category.htm")  || url.contains("m.tmall.com/shop/shop_auction_search.do")
                    || url.contains("https://s.m.taobao.com/search")) {
                error_count = 0;

                mWebView.evaluateJavascript(Injection, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.i(TAG, "详情结果：     " + s);
                        if (s.length() < 1000 && s.contains("哎哟喂,被挤爆啦,请稍后重试!")) {
                            mHandler.sendEmptyMessageDelayed(GET, 100);
                        }
                        result = s;
                        mHandler.sendEmptyMessage(PUSH);
                    }
                });
            }  else  if (url.contains("taobao.com/i/asynSearch.htm")){
                // add by zuozhuang 2018.04.02   begin
                error_count = 0;
                mWebView.evaluateJavascript(GetDataId, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.i(TAG, "详情结果：     " + s);
                        if (s.length() < 1000 && s.contains("哎哟喂,被挤爆啦,请稍后重试!")) {
                            mHandler.sendEmptyMessageDelayed(GET, 100);

                        }
                        result = s;
                        mHandler.sendEmptyMessage(PUSH);
                    }
                });
                // add by zuozhuang 2018.04.02  end
            }else if (url.contains("tmall.com/index.htm")) {
                error_count = 0;

                injection();
            }else if (url.contains("https://list.tmall.com/search_product.htm")) {
                Log.i(TAG, "网页链接");
                mWebView.loadUrl("javascript:Tmall_zz.processHTML(document.documentElement.outerHTML);");

            } else {
                mHandler.sendEmptyMessageDelayed(GET, 3000);
                Log.e(TAG, "url 错误" );
            }

        }
    };

    public void sleep(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldOverrideUrlLoadingByApp(String url) {
        Log.i(TAG, "shouldOverrideUrlLoading    " + url);
        if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp")) {
            return false;
        }
        return true;
    }


    private void imageByte(String url) {

        String[] ss = url.split("&");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].contains("type=")) {
                ss[i] = "type=default";
            }
            sb.append(ss[i] + "&");
        }

        url = sb.substring(0, sb.length() - 1);
        Log.i(TAG, "请求的验证码是：" + url);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        OkHttpClient mOkHttpClient = builder.build();
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                InputStream is = null;

                is = response.body().byteStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int numBytesRead = 0;
                while ((numBytesRead = is.read(buf)) != -1) {
                    output.write(buf, 0, numBytesRead);
                }
                results = output.toByteArray();
                Log.i(TAG, "byte  is " + new String(results));
                is.close();
                output.flush();
                output.close();
                verification();
            }
        });
    }

    private void verification() {


        Log.i(TAG, "开始获取验证码结果");

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        OkHttpClient mOkHttpClient = builder.build();
        RequestBody body = RequestBody.create(null, results);
        Request request = new Request.Builder()
                .url("http://120.25.209.119:6324/image/captcha/bitspaceman")
                .post(body)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String str = response.body().string();//获取到返回的json,错误{"msg": "internal error"}


                Log.i(TAG, "返回的验证码是：" + str);
                if (str.contains("internal error")) {
                    mHandler.sendEmptyMessage(LOAD);
                    return;
                } else {
                    try {
                        JSONObject json = new JSONObject(str);
                        Message message = new Message();
                        message.obj = json.getString("label");
                        message.what = INPUT;
                        mHandler.sendMessage(message);
                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                }

            }
        });
    }

    private void injection() {
        mWebView.evaluateJavascript("javascript:RegExp('name=\"shopId\" value=\".+\"').exec(document.body.innerHTML)[0]", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {

                Log.i(TAG, "value is " + value);

                if (null != value && value.length() > 5) {
                    index = 0;
                    result = value;

                    mHandler.sendEmptyMessage(PUSH);
                } else {

                    Log.i(TAG, "没有取到结果，再取任务");
                    if (index < 4) {
                        mHandler.sendEmptyMessageDelayed(injection, 500);
                        index++;
                    } else {
                        new ErrorTask().execute();
                    }
                }
            }
        });
    }

    private class AuthTask extends AsyncTask<Void, Void, Void> {

        @SuppressLint("LongLogTag")
        @Override
        protected Void doInBackground(Void... params) {
            try {
                jedis = new Jedis(Config.HOST, Config.PORT, Config.TIMEOUT);
                jedis.auth(Config.PASSWORD);
                jedis.select(2);
                Log.i(TAG, "tmall JEDIS Auth");
                mHandler.sendEmptyMessage(GET);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    jedis.close();
                    jedis.disconnect();
                }catch (Exception a){

                }
                mHandler.sendEmptyMessageDelayed(AUTH, 30000);
            }
            return null;
        }

    }

    private class GetTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {

                String json = jedis.rpop("tmall_render_task1");

                if (json != null) {

                    JSONObject j = new JSONObject(json);

                    taskUrl = j.getString("url");
                    taskId = j.getString("id");
                    long t = j.getLong("t");

                    if (taskUrl != null & !"".equals(taskUrl)) {

                        if (t < (System.currentTimeMillis() / 1000 - 20)) {
                            Log.i(TAG, "超时");
                            mHandler.sendEmptyMessage(GET);
                        } else {
                            acceptTime = System.currentTimeMillis();
                            mHandler.sendEmptyMessage(LOAD);

                        }

                    } else {
                        Log.i(TAG, "数据不符合规范");
                        mHandler.sendEmptyMessageDelayed(GET, 2000);
                    }
                } else {
                    Log.v(TAG, "没有取到数据");
                    mHandler.sendEmptyMessageDelayed(GET, 1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    jedis.close();
                    jedis.disconnect();
                }catch (Exception a){

                }

                mHandler.sendEmptyMessageDelayed(AUTH, 2000);
                mHandler.sendEmptyMessageDelayed(GET, 2000);

            }
            return null;
        }

    }

    private class PushTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                //
                Log.i(TAG, "异步线程取到的结果：   " + result);
                jedis.setex("tmall_render_" + taskId, 60*5, result);
                Log.i(TAG, "上传完成-----------------------------------");
                long doTime = System.currentTimeMillis() - acceptTime;
                mHandler.sendEmptyMessageDelayed(GET, 2000 - doTime);//增加10秒取任务间隔
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.sendEmptyMessageDelayed(AUTH, 2000);
                mHandler.sendEmptyMessageDelayed(PUSH, 2000);
            }
            return null;
        }

    }

    private class PushCookie extends AsyncTask<String , Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                //
                Log.i(TAG, "异步线程取到的结果：   " + params[0]);
                jedis.set("Tmall:tmall_cookie"+System.currentTimeMillis(), params[0], "NX", "EX", 600);
                //jedis.expire("tmall_cookie",1800);
                Log.i(TAG, "上传cookie完成-----------------------------------");
                //long doTime = System.currentTimeMillis() - acceptTime;
                //mHandler.sendEmptyMessageDelayed(GET, 2000 - doTime);//增加10秒取任务间隔
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.sendEmptyMessageDelayed(AUTH, 1000);
                sleep(2);
                new PushCookie().execute(params[0]);
            }
            return null;
        }

    }

    private class ErrorTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                //
                Log.i(TAG, "异步线程取到的结果：   " + result);
                jedis.hset("tmall_render_error", taskUrl, "1");
                Log.i(TAG, "上传完成-----------------------------------");
                mHandler.sendEmptyMessageDelayed(GET, 100);
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.sendEmptyMessageDelayed(AUTH, 2000);
                mHandler.sendEmptyMessageDelayed(PUSH, 2000);
            }
            return null;
        }

    }

    /**
     * 签到
     */
    private class SignTask extends AsyncTask<Void, Void, Void> {

        int mNum = 1;

        @SuppressLint("LongLogTag")
        protected Void doInBackground(Void... params) {
            String LINK = "http://120.25.255.56/mobileapi/sign?mid=" + Util.getDeviceID(MainActivity.this) +
                    "&cpu=" + Util.getcpu(MainActivity.this) +
                    "&memory=" + Util.getAvailMemory(MainActivity.this) +
                    "&storage=" + Util.getMennoryRate() +
                    "&battery=" + Util.getBattery(MainActivity.this) +
                    "&key=" + Config.ORIGINAL_KEY +
                    "&temperature=" + Util.getTemperature(MainActivity.this) +
                    "&appcode=tmall";

            try {
                URL url = new URL(LINK);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    Log.i(TAG, "postPhoneState Success");
                    if (mNum == -1 || mSignKey.equals("")) {
                        String json = Util.convertStreamToString(connection.getInputStream());
                        Log.i(TAG, "json is " + json);
                        JSONObject jb = new JSONObject(json);
                        if (jb.optInt("error") == 0) {
                            mSignKey = jb.getString("signkey");
                            mHandler.sendEmptyMessage(UPDATE);
                            Log.i(TAG, "SignTask key is: " + mSignKey);
                        } else {
                            //返回异常
                            mSignKey = "";
                            Log.i(TAG, "Exception ,error message is:  " + jb.getString("msg"));
                        }
                    }
                } else {
                    Log.i(TAG, "postPhoneState Fail,responseCode is:  " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "postPhoneState Fail");
            }
            return null;
        }
    }

    class RebootTask extends AsyncTask<Void, Void, Void> {
        @SuppressLint("LongLogTag")
        @Override
        protected Void doInBackground(Void... params) {

            String LINK = "http://120.25.255.56/mobileapi/checkreboot?mid=" + Util.getDeviceID(MainActivity.this);
            try {
                URL url = new URL(LINK);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    Log.i(TAG, "RebootTask request Success");
                    String json = Util.convertStreamToString(connection.getInputStream());
                    JSONObject jo = new JSONObject(json);
                    Log.i(TAG, json);
                    if (jo.getInt("error") == 0) {
                        int code = jo.getInt("need_reboot");

                        if (code == 1) {
                            //重启任务
                            mHandler.sendEmptyMessage(CLOSE);
                        }
                    }

                } else {
                    Log.i(TAG, "RebootTask request fail,responseCode is:  " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();

                Log.i(TAG, "RebootTask request fail");
            }
            return null;
        }
    }

    /**
     * 更新
     */
    private class UpdateTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (getLocationVersionCode() < getServerVersionCode()) {
                DownloadApk();
            }
            return null;
        }

        @SuppressLint("LongLogTag")
        private int getLocationVersionCode() {
            try {
                PackageInfo packageInfo = MainActivity.this.getPackageManager().getPackageInfo(MainActivity.this.getPackageName(), 0);
                Log.i(TAG, "location version code is " + packageInfo.versionCode);
                return packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        }

        @SuppressLint("LongLogTag")
        private int getServerVersionCode() {
            HttpURLConnection connection = null;
            String link = "http://120.25.255.56/mobileapi/appinfo?mid=" + Util.getDeviceID(MainActivity.this) +
                    "&sign=" + Util.md5(mSignKey + Util.getDeviceID(MainActivity.this)) +
                    "&appcode=Tmall";
            try {
                Log.i(TAG, "get Server Version Code url is: " + link);
                URL url = new URL(link);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    String json = Util.convertStreamToString(connection.getInputStream());
                    Log.i(TAG, "server response body is:  " + json);
                    JSONObject result = new JSONObject(json);
                    Log.i(TAG, "server version code is " + result.optInt("version"));
                    if (result.optInt("error") == 0) {
                        return result.optInt("version");
                    } else {
                        Log.i(TAG, "request server version code fail,error meaasge is:  " + result.getString("msg"));
                    }
                } else {
                    Log.i(TAG, "error response code is: " + responseCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            return -1;
        }

        @SuppressLint("LongLogTag")
        private void DownloadApk() {
            String link = "http://120.25.255.56/mobileapi/getappurl?mid=" + Util.getDeviceID(MainActivity.this) +
                    "&sign=" + Util.md5(mSignKey + Util.getDeviceID(MainActivity.this)) +
                    "&appcode=Tmall";
            String download = "";
            HttpURLConnection connection = null;
            try {
                URL url = new URL(link);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    JSONObject result = new JSONObject(Util.convertStreamToString(connection.getInputStream()));
                    if (result.optInt("error") == 0) {
                        download = result.getString("url");
                        Log.i(TAG, download);


                    } else {
                        Log.i(TAG, "get download url fail,error message is: " + result.getString("msg"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            DownloadManager systemService = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download));
            FileUtils.deleteDir(new File(Environment.getExternalStorageDirectory(), "Tmall"));
            request.setDestinationInExternalPublicDir("Tmall", "Tmall.apk");
            mEnqueue = systemService.enqueue(request);
            mHandler.sendEmptyMessage(DOWN);
            registerReceiver(mDownLoadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    /**
     * 下载服务
     */
    class DownLoadReceiver extends BroadcastReceiver {
        @SuppressLint("LongLogTag")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (0 == mSuccess++ && intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long longExtra = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (longExtra == mEnqueue) {
                    Log.i(TAG, "Download success!");
                    mHandler.sendEmptyMessage(SHOW);
                    try {
                        shell.execute("mount -o rw,remount /system\n");
                        Thread.sleep(2000);
                        shell.execute("mv -f /sdcard/Tmall/Tmall.apk /system/priv-app/Tmall/\n");
                        Thread.sleep(5000);
                        shell.execute("chgrp root /system/priv-app/Tmall/Tmall.apk\n");
                        Thread.sleep(2000);
                        shell.execute("chmod 755 /system/priv-app/Tmall/Tmall.apk\n");
                        Thread.sleep(2000);
                        shell.execute("reboot\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    unregisterReceiver(mDownLoadReceiver);
                }

            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null)
            mWebView.destroy();
        mHandler.removeMessages(RESTART);
        mHandler.removeCallbacksAndMessages(null);
    }
}
