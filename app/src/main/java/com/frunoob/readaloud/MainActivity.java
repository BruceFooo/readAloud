package com.frunoob.readaloud;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.frunoob.readaloud.entity.Book;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static AppDatabase db;
    private static final String TAGName = "MainActivity";
    private static final String rootDir = "/storage/self/primary";

    private TextToSpeech textToSpeech;
    private static Book book;
    private static final String COLON = ":";
    private EditText et_page;
    private TextView bookTitle;

    public static int readingPage = 0;

    ActivityResultLauncher<String> openBookLauncher =  registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
//        Log.d(TAGName,"uri:"+result);
        Uri uri = result;
        Log.d(TAGName,uri.getPath());
        new Thread(()->{
            book = db.bookDao().findByUri(uri.getPath());
            if (book == null){
                book = new Book();
                book.setUri(uri.getPath());
                book.name = uri.getPath().split("/")[uri.getPath().split("/").length-1];
                Log.d(TAGName,"book:"+book.name);
                db.bookDao().insertAll(book);
                book = db.bookDao().findByUri(uri.getPath());
            }
            // 更新视图
            runOnUiThread(()->{
                et_page.setText(String.valueOf(book.paperNumber));
                bookTitle.setText(book.name);
            });
        }).start();

    } );


    private void readPdfFile(int page) {



        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d(TAGName, "Permission granted");
            } else {
                //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri1 = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri1);
                startActivity(intent);
            }
        }


        String logTitle = "readFile";
        String fullPath="";
        try {
            fullPath = String.format("%s/%s", rootDir, book.uri.split(COLON)[1]);
            Log.d("文件完整路径",fullPath);
            PdfReader pdfReader = new PdfReader(fullPath);
            int pages =pdfReader.getNumberOfPages();


            // 读取每一页的文本
            new Thread(()->{
                for (int i = page; i<pages ;i++){
                    try {
                        Log.d(TAGName,"reading page:"+i);
                        textToSpeech.speak(PdfTextExtractor.getTextFromPage(pdfReader,i).trim(),TextToSpeech.QUEUE_FLUSH,null,null);
                        book.paperNumber = i;
                        db.bookDao().update(book);

                        readingPage = i;


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // 阻塞线程直到说完
                    while (textToSpeech.isSpeaking()){

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (findViewById(R.id.btn_read).getTag().equals("pause")){
                        break;
                    }

                }
                pdfReader.close();
            }).start();
        }catch (NullPointerException e){
            Toast.makeText(getApplicationContext(),"路径不存在",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.w(TAGName,e.getMessage()==null?"出错了，未知异常":e.getMessage());
        }

//        if (!fullPath.isEmpty()) Toast.makeText(getApplicationContext(),fullPath,Toast.LENGTH_SHORT).show();
//        else Toast.makeText(getApplicationContext(),"文件路径获取失败",Toast.LENGTH_SHORT).show();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_page = findViewById(R.id.et_page);
        Log.d(TAGName,Environment.getRootDirectory().getAbsolutePath());
        // 初始化数据库，仅一次
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class,"read-aloud").build();

        textToSpeech = new TextToSpeech(getApplicationContext(),status -> {
            textToSpeech.setLanguage(Locale.SIMPLIFIED_CHINESE);
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBookLauncher.launch("*/*");
            }
        });

        // 为按钮设置点击事件
        findViewById(R.id.btn_read).setOnClickListener(v -> {
            if (textToSpeech.isSpeaking()){
                textToSpeech.stop();
                runOnUiThread(()->{
                    et_page.setText(String.valueOf(readingPage));
                });
                ((TextView)findViewById(R.id.btn_read)).setText("朗读");
                findViewById(R.id.btn_read).setTag("pause");
            }else{

                int page = Integer.parseInt(et_page.getText().toString());
                readPdfFile(page);
                // 修改朗读按钮文本为"暂停"
                ((TextView)findViewById(R.id.btn_read)).setText("暂停");
                findViewById(R.id.btn_read).setTag("read");
            }
        });

        bookTitle = findViewById(R.id.book_title);

        // 为电源优化白名单按钮设置点击事件
        findViewById(R.id.btn_power_optimization).setOnClickListener(this::removePowerOptimization);

        // 为设置语音引擎按钮设置点击事件
        findViewById(R.id.btn_set_tts).setOnClickListener(this::setTts);

    }

    // 电源优化白名单
    public void setPowerOptimization(View view) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // 从电源优化名单中移除
    @SuppressLint("BatteryLife")
    public void removePowerOptimization(View view) {
       PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(getPackageName()) ){
            Toast.makeText(getApplicationContext(),"已经在白名单中",Toast.LENGTH_SHORT).show();
        } else {
            // 申请加入白名单
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:"+getPackageName()));
            startActivity(intent);
        }
    }





    // 修改系统默认的朗读引擎
    public void setTts(View view) {
        Intent intent = new Intent();
        intent.setAction("com.android.settings.TTS_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // 退出程序时释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        textToSpeech.shutdown();
    }

}
