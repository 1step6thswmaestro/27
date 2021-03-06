package com.wraptalk.wraptalk.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.wraptalk.wraptalk.R;
import com.wraptalk.wraptalk.adapter.FavoriteChannelAdapter;
import com.wraptalk.wraptalk.models.ChannelData;
import com.wraptalk.wraptalk.utils.DBManager;

import java.util.ArrayList;

public class FavoriteChannelActivity extends AppCompatActivity {

    View view;
    ArrayList<ChannelData> source;
    FavoriteChannelAdapter customAdapter = null;
    ListView listView_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_channel);

        initModel();
        initController();
        getChannelList();
    }

    private void initModel() {
        source = new ArrayList<>();
        listView_result  = (ListView) findViewById(R.id.listView_favoriteChannel);
    }

    private void initController() {
        customAdapter = new FavoriteChannelAdapter(this, source);
        listView_result.setAdapter(customAdapter);
    }

    private void getChannelList() {
        DBManager.getInstance().select("SELECT * FROM chat_info", new DBManager.OnSelect() {
            @Override
            public void onSelect(Cursor cursor) {
                ChannelData data = new ChannelData();
                data.setApp_id(cursor.getString(cursor.getColumnIndex("app_id")));
                data.setChannel_id(cursor.getString(cursor.getColumnIndex("channel_id")));
                data.setChannel_name(cursor.getString(cursor.getColumnIndex("channel_name")));
                source.add(data);
            }

            @Override
            public void onComplete(int cnt) {
                customAdapter.notifyDataSetChanged();
            }

            @Override
            public void onErrorHandler(Exception e) {

            }
        });
    }
}