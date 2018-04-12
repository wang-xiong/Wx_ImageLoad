package com.baidu.duer.dcs.guo_imagload;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.guo_list_view);
        BaseAdapter adapter = new ImageAdapter(this, 0, Images.iamgeUrls);
        listView.setAdapter(adapter);
    }
}
