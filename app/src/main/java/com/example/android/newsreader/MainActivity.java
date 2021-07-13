package com.example.android.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("com.example.android.newsreader",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,aricleId INTEGER,title VARCHAR,content VARCHAR)");
        ListView listView = findViewById(R.id.listView);
        String result="";
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        DownloadTask downloadTask = new DownloadTask();
        try {
           //downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateListView();
        //Log.i("Result",result);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });
    }

    public void updateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();
        }

        do {
            titles.add(c.getString(titleIndex));
            content.add(c.getString(contentIndex));

        }while (c.moveToNext());
        arrayAdapter.notifyDataSetChanged();
    }


class DownloadTask extends AsyncTask<String,Void,String>{

    @Override
    protected String doInBackground(String... urls) {
        String result="";
        URL url= null;
        HttpURLConnection connection = null;
        String articleTitle = null;

        try {
            url = new URL(urls[0]);
            connection = (HttpURLConnection)url.openConnection();
            InputStream in = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(in);
            int data = reader.read();
            while (data != -1){
                char c = (char) data;
                result+=c;
                data=reader.read();
            }

            JSONArray jsonArray = new JSONArray(result);
            int noOfItems = 20;
            if (jsonArray.length() < 20)
                noOfItems = jsonArray.length();

            articleDB.execSQL("DELETE FROM articles");

            for (int i =0; i<20;i++){
                String articleId = jsonArray.getString(i);

                url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                connection = (HttpURLConnection)url.openConnection();
                in = connection.getInputStream();
                reader = new InputStreamReader(in);
                data = reader.read();
                String articleInfo = "";
                while (data != -1){
                    char c = (char) data;
                    articleInfo+=c;
                    data=reader.read();
                }
                //Log.i("Articles", articleInfo);
                JSONObject jsonObject = new JSONObject(articleInfo);
                if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                    articleTitle = jsonObject.getString("title");
                    String articleUrl = jsonObject.getString("url");
                    Log.i("Title & URL ", articleTitle + articleUrl);

                    url = new URL(articleUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    in = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(in);
                    data = inputStreamReader.read();
                    String articleContent = "";
                    while (data != -1) {
                        char c = (char) data;
                        articleContent += c;
                        data = inputStreamReader.read();
                    }
                    //Log.i("artile content", articleContent);

                    String sql = "insert into articles(aricleId, title, content) values(?, ?, ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, articleTitle);
                    statement.bindString(3, articleContent);
                    statement.execute();
                }
            }
            //Log.i("Results", result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("bk", "failed");
            return "Failed";
        }
    }
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        updateListView();
    }
}}