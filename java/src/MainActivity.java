package com.example.myandroidtvaudiotest;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int msgKey1 = 1;

    private Button play = null;
    private Button down = null;
    private Button up = null;
    private Button sea = null;
    private ProgressBar pb = null;

    private TextView cv;
    private TextView tv;

    private int maxVolume = 50;
    int curVolume = 20;
    private int stepVolume = 0; // 每次调整的音量幅度

    private MediaPlayer mediaPlayer = null;// 播放器
    private AudioManager audioMgr = null; // Audio管理器，用了控制音量
    private AssetManager assetMgr = null; // 资源管理器

    String musicPath = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        new TimeThread().start();
        audioWork();
    }


    //初始化界面
    private void initUI() {
        play = findViewById(R.id.Play);
        down = findViewById(R.id.Down);
        up = findViewById(R.id.up);
        sea = findViewById(R.id.search);

        cv = findViewById(R.id.currentvolume);
        cv.setText(curVolume+"");
        tv = findViewById(R.id.list);

        play.setOnClickListener(this);
        down.setOnClickListener(this);
        up.setOnClickListener(this);

        pb = findViewById(R.id.Volume);
        pb.setMax(maxVolume);
        pb.setProgress(curVolume);


        sea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
            }
        });
    }

    //获取系统音频参数
    private void audioWork() {
        audioMgr = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        curVolume = maxVolume / 2;
        stepVolume = maxVolume / 10;

        mediaPlayer = new MediaPlayer();
        assetMgr = this.getAssets();
    }

    //进行播放
    private void getAndPlay() {
        try {
            // 打开指定音乐文件
            AssetFileDescriptor afd = assetMgr.openFd(musicPath);
            mediaPlayer.reset();
            // 使用MediaPlayer加载指定的声音文件。
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
            // 准备声音
            mediaPlayer.prepare();
            // 播放
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //音量调节反馈
    private void adjustVolume() {
        audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume,
                AudioManager.FLAG_PLAY_SOUND);
    }


    //音量调节
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.Play:
                getAndPlay();
                break;
            case R.id.Down:
                curVolume -= stepVolume;
                if (curVolume <= 0) {
                    curVolume = 0;
                }
                pb.setProgress(curVolume);
                break;
            case R.id.up:
                curVolume += stepVolume;
                if (curVolume >= maxVolume) {
                    curVolume = maxVolume;
                }
                pb.setProgress(curVolume);
                break;
            default:
                break;
        }

        adjustVolume();
    }

    //选择文件反馈
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //是否选择，没选择就不会继续
        if (resultCode == Activity.RESULT_OK) {
            //得到uri，后面就是将uri转化成file的过程。
            Uri uri = data.getData();
            //使用第三方应用打开
            if ("file".equalsIgnoreCase(uri.getScheme())){
                musicPath = uri.getPath();
                tv.setText(musicPath);
                Toast.makeText(this,musicPath+" ^-^",Toast.LENGTH_SHORT).show();
                return;
            }
            //4.4以后
            else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                musicPath = getPath(this, uri);
                tv.setText(musicPath);
                Toast.makeText(this,musicPath,Toast.LENGTH_SHORT).show();
            }
        }
    }

    //获得文件路径
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    //生成路径
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    //来源判断
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    //线程通过handle发送信息
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage (Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgKey1:
                    cv.setText(curVolume+"");
                    break;

                default:
                    break;
            }
        }
    };

    //创建一个线程，每0.1秒向activity传送信息
    public class TimeThread extends Thread {
        @Override
        public void run () {
            do {
                try {
                    Thread.sleep(100);
                    Message msg = new Message();
                    msg.what = msgKey1;
                    mHandler.sendMessage(msg);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(true);
        }
    }

}

