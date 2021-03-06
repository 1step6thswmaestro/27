package com.wraptalk.wraptalk.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFlat;
import com.wraptalk.wraptalk.R;
import com.wraptalk.wraptalk.adapter.ChannelAdapter;
import com.wraptalk.wraptalk.models.ChannelData;
import com.wraptalk.wraptalk.models.UserInfo;
import com.wraptalk.wraptalk.utils.AppSetting;
import com.wraptalk.wraptalk.utils.DBManager;
import com.wraptalk.wraptalk.utils.OnRequest;
import com.wraptalk.wraptalk.utils.RequestUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

public class ChannelActivity extends AppCompatActivity {

    ArrayList<ChannelData> source;
    ChannelAdapter customAdapter = null;
    ListView listView_result;

    EditText editText_searchChannel;
    ButtonFlat button_search;

    PackageInfo packageInfo;
    String categoryName;
    String searchKeyword;

    ChannelData channelData  = new ChannelData();
    String url;

    String app_id, app_name, nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);

        Intent intent = getIntent();
        packageInfo = (PackageInfo) intent.getExtras().get("packageInfo");
        categoryName = intent.getStringExtra("categoryName");

        initModel();
        getChannelList();
        initController();


        getSupportActionBar().setTitle("채널 가입");

        button_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchKeyword = editText_searchChannel.getText().toString();
                searchChannel();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_changeNickname :
                showChangeNickDialog();
                break;
            case R.id.action_createChannel :
                showCreateChannelDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initModel() {
        source = new ArrayList<>();
        listView_result  = (ListView) findViewById(R.id.listVeiw_channel);
        editText_searchChannel = (EditText) findViewById(R.id.editText_searchChannel);
        button_search = (ButtonFlat) findViewById(R.id.button_search);
        if(categoryName != null && categoryName.startsWith(TabCategoryFragment.PRE_CHANNEL_PREFIX)) {
            app_id = categoryName;
            app_name = categoryName.replace(TabCategoryFragment.PRE_CHANNEL_PREFIX, "");
        }
        else {
            app_id = packageInfo.packageName;
            app_name = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
        }
        channelData.setApp_id(app_id);

        // 닉네임 select해서 가져오기
        DBManager.getInstance().select("SELECT * FROM app_info WHERE app_id='" + app_id + "';", new DBManager.OnSelect() {
            @Override
            public void onSelect(Cursor cursor) {
                nickname = cursor.getString(cursor.getColumnIndex("user_nick"));
                Log.e("nickname", nickname);
            }

            @Override
            public void onComplete(int cnt) {

            }

            @Override
            public void onErrorHandler(Exception e) {
            }
        });
    }

    private void initController() {
        customAdapter = new ChannelAdapter(this, source);
        listView_result.setAdapter(customAdapter);
    }

    private void showChangeNickDialog() {

        // Diaolog 생성
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_nickname, null);
        PackageManager packageManager = this.getPackageManager();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText_nickname = (EditText) dialogView.findViewById(R.id.editText_nickname);

        builder.setView(dialogView);

        url = AppSetting.REST_URL + "/user/registNick?token=" + UserInfo.getInstance().token + "&app_id=" + app_id;

        if(packageInfo != null) {
            builder.setIcon(packageManager.getApplicationIcon(packageInfo.applicationInfo));
            builder.setTitle(app_name);
        }

        TextWatcher watcher = new TextWatcher() {
            String text;
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                text = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {
                int length = s.toString().length();
                if( length > 0 ){
                    Pattern ps = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣ\\u318D\\u119E\\u11A2\\u2025a.-zA-Z]*$");//영문, 숫자, 한글만 허용
                    if(!ps.matcher(s).matches()){
                        editText_nickname.setText(text);
                        editText_nickname.setSelection(editText_nickname.length());
                    }
                }
            }
        };
        editText_nickname.addTextChangedListener(watcher);

        builder.setPositiveButton("SET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (editText_nickname.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    url += "&user_nick=";
                    try {
                        url += URLEncoder.encode(editText_nickname.getText().toString(), "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    RequestUtil.asyncHttp(url, new OnRequest() {
                        @Override
                        public void onSuccess(String url, byte[] receiveData) {
                            String query = "UPDATE app_info SET user_nick='" + editText_nickname.getText().toString() + "' WHERE app_id='" + app_id + "'";
                            DBManager.getInstance().write(query);
                            Toast.makeText(ChannelActivity.this, "닉네임 변경에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFail(String url, String error) {
                            Toast.makeText(ChannelActivity.this, "변경 실패 : 중복된 닉네임입니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = builder.create();

        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

        //Dialog 보이기
        dialog.show();
    }

    private void showCreateChannelDialog() {

        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_channel, null);

        final CheckBox checkBox_channelOnoff = (CheckBox) dialogView.findViewById(R.id.checkBox_channelOnoff);
        final EditText editText_setPassword = (EditText) dialogView.findViewById(R.id.editText_setPassword);
        final TextView textView_password = (TextView) dialogView.findViewById(R.id.textView_password);

        textView_password.setVisibility(View.INVISIBLE);
        editText_setPassword.setVisibility(View.INVISIBLE);

        checkBox_channelOnoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    textView_password.setVisibility(View.INVISIBLE);
                    editText_setPassword.setVisibility(View.INVISIBLE);
                } else {
                    textView_password.setVisibility(View.VISIBLE);
                    editText_setPassword.setVisibility(View.VISIBLE);
                }
            }
        });


        final Spinner spinner_userCount = (Spinner) dialogView.findViewById(R.id.spinner_userCount);
        ArrayAdapter<CharSequence> adapter;

        ArrayList<CharSequence> list = new ArrayList<>();
        for(int i = 1; i < 101 ; i++)
            list.add(String.valueOf(i));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_userCount.setAdapter(adapter); // OK!!
        spinner_userCount.setSelection(19);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("채널 생성");
        builder.setView(dialogView);

        spinner_userCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                channelData = new ChannelData();
                channelData.setChannel_limit(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(ChannelActivity.this, "인원을 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                getNewChannelInfo(dialogView);
                if(channelData.getChannel_name().isEmpty()) {
                    Toast.makeText(ChannelActivity.this, "채널 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else {
                    RequestUtil.asyncHttp(url, new OnRequest() {
                        @Override
                        public void onSuccess(String url, byte[] receiveData) {
                            String jsonStr = new String(receiveData);
                            String query;
                            try {
                                JSONObject json = new JSONObject(jsonStr);
                                channelData.setChannel_id(json.optString("channel_id"));

                                query = String.format( "INSERT INTO chat_info " +
                                                "(channel_id, public_onoff, channel_limit, channel_cate, " +
                                                "app_id, channel_name, chief_id, user_color, check_favorite) " +
                                                "VALUES ('%s', '%s', '%d', '%s', '%s', '%s', '%s', '%s', %d)",
                                        channelData.getChannel_id(), channelData.getPublic_onoff(), channelData.getChannel_limit(), channelData.getChannel_cate(),
                                        app_id, channelData.getChannel_name(), channelData.getChief_id(), channelData.getUser_color(), 0);

                                DBManager.getInstance().write(query);
                                source.add(channelData);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Collections.sort(source);
                            customAdapter.notifyDataSetChanged();
                            Toast.makeText(ChannelActivity.this, "채널 생성에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFail(String url, String error) {
                            Toast.makeText(ChannelActivity.this, "채널 생성에 실패하셨습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = builder.create();

        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

        //Dialog 보이기
        dialog.show();
    }

    private void getNewChannelInfo(View v) {

        final EditText editText_channelName = (EditText) v.findViewById(R.id.editText_channelName);
        final EditText editText_setPassword = (EditText) v.findViewById(R.id.editText_setPassword);
        final CheckBox checkBox_channelOnoff = (CheckBox) v.findViewById(R.id.checkBox_channelOnoff);


        channelData.setChannel_name(editText_channelName.getText().toString());
        /* url 생성 */

        url = AppSetting.REST_URL + "/user/makeChannel?token=" + UserInfo.getInstance().token + "&app_id=" + app_id;

        try {
            url += "&channel_name=" + URLEncoder.encode(channelData.getChannel_name(), "utf-8") + "&public_onoff=";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(checkBox_channelOnoff.isChecked()) {
            url += "on";
            channelData.setPublic_onoff("on");
        }
        else {
            url += "off&channel_pw=" + editText_setPassword.getText().toString();
            channelData.setPublic_onoff("off");
        }

        channelData.setChief_id(UserInfo.getInstance().email);
        channelData.setUser_color("#000000");
        channelData.setCheck_registeration(1);
        channelData.setCheck_favorite(0);
        channelData.setUser_nick(nickname);

        try {
            url += "&channel_limit=" + channelData.getChannel_limit() + "&user_nick=" + URLEncoder.encode(nickname, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void getChannelList() {

        url = AppSetting.REST_URL + "/user/appChannel?" + "token=" + UserInfo.getInstance().token + "&app_id=" + app_id;

        RequestUtil.asyncHttp(url, new OnRequest() {
            @Override
            public void onSuccess(String url, byte[] receiveData) {
                String jsonStr = new String(receiveData);
                try {
                    JSONObject json = new JSONObject(jsonStr);
                    int result_code = json.optInt("result_code", -1);

                    if (result_code != 0) {
                        String result_msg = json.optString("result_msg", "fail");
                        Toast.makeText(getApplicationContext(), result_msg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONArray list_channel = json.optJSONArray("list_channel");
                    for (int i = 0; i < list_channel.length(); i++) {
                        JSONObject channelObj = list_channel.getJSONObject(i);

                        final ChannelData data = new ChannelData();

                        data.setChannel_id(channelObj.optString("channel_id"));
                        data.setPublic_onoff(channelObj.optString("public_onoff"));
                        data.setChannel_limit(channelObj.optInt("channel_limit"));
                        data.setChannel_cate(channelObj.optString("channel_cate"));
                        data.setApp_id(channelObj.optString("app_id"));
                        data.setChannel_name(channelObj.optString("channel_name"));
                        // userNick은 app_info에서 갖다가 써야 함
                        data.setUser_color(channelObj.optString("user_color"));
                        data.setChief_id(channelObj.optString("chief_id"));

                        String query = "SELECT * FROM chat_info WHERE app_id='" + app_id + "';";
                        DBManager.getInstance().select(query, new DBManager.OnSelect() {
                            @Override
                            public void onSelect(Cursor cursor) {
                                String id1 = data.getChannel_id();
                                String id2 = cursor.getString(cursor.getColumnIndex("channel_id"));
                                Log.e("onSelect", id1 + "," + id2);

                                if (id1.equals(id2)) {
                                    data.setCheck_registeration(1);
                                } else if (!id1.equals(id2) && data.getCheck_registeration() != 1) {
                                    data.setCheck_registeration(0);
                                }
                            }

                            @Override
                            public void onComplete(int cnt) {
                            }

                            @Override
                            public void onErrorHandler(Exception e) {

                            }
                        });
                        source.add(data);
                    }
                    Collections.sort(source);
                    customAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFail(String url, String error) {

            }
        });
    }

    private void searchChannel() {

        String url = AppSetting.REST_URL + "/user/appChannel?" + "token=" + UserInfo.getInstance().token + "&channel_name=" + searchKeyword + "&app_id=" + app_id;

        RequestUtil.asyncHttp(url, new OnRequest() {
            @Override
            public void onSuccess(String url, byte[] receiveData) {
                String jsonStr = new String(receiveData);
                source.removeAll(source);

                try {
                    JSONObject json = new JSONObject(jsonStr);
                    int result_code = json.optInt("result_code", -1);

                    if (result_code != 0) {
                        String result_msg = json.optString("result_msg", "fail");
                        Toast.makeText(getApplicationContext(), result_msg, Toast.LENGTH_SHORT).show();

                        return;
                    }

                    JSONArray list_channel = json.optJSONArray("list_channel");
                    for (int i = 0; i < list_channel.length(); i++) {
                        JSONObject channelObj = list_channel.getJSONObject(i);

                        final ChannelData data = new ChannelData();

                        data.setChannel_id(channelObj.optString("channel_id"));
                        data.setPublic_onoff(channelObj.optString("public_onoff"));
                        data.setChannel_limit(channelObj.optInt("channel_limit"));
                        data.setChannel_cate(channelObj.optString("channel_cate"));
                        data.setApp_id(channelObj.optString("app_id"));
                        data.setChannel_name(channelObj.optString("channel_name"));
                        // userNick은 app_info에서 갖다가 써야 함
                        // user_color는 chat에서
                        data.setChief_id(channelObj.optString("chief_id"));

                        DBManager.getInstance().select("SELECT * FROM chat_info WHERE app_id='" + app_id + "';", new DBManager.OnSelect() {
                            @Override
                            public void onSelect(Cursor cursor) {
                                String id1 = data.getChannel_id();
                                String id2 = cursor.getString(cursor.getColumnIndex("channel_id"));
                                Log.e("onSelect", id1 + "," + id2);

                                if (id1.equals(id2)) {
                                    data.setCheck_registeration(1);
                                } else if (!id1.equals(id2) && data.getCheck_registeration() != 1) {
                                    data.setCheck_registeration(0);
                                }
                            }

                            @Override
                            public void onComplete(int cnt) {
                            }

                            @Override
                            public void onErrorHandler(Exception e) {

                            }
                        });
                        source.add(data);
                    }
                    Collections.sort(source);
                    customAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFail(String url, String error) {

            }
        });
    }
}
