package com.dds.core.ui.local;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dds.App;
import com.dds.core.MainActivity;
import com.dds.core.base.BaseActivity;
import com.dds.core.consts.Urls;
import com.dds.core.socket.IUserState;
import com.dds.core.socket.SocketManager;
import com.dds.core.util.AppRTCUtils;
import com.dds.core.util.UdpSocketUtils;
import com.dds.core.voip.CallSingleActivity;
import com.dds.skywebrtc.Logger;
import com.dds.webrtc.R;

import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends BaseActivity implements IUserState {
    private Toolbar toolbar;
    private EditText etUser;
    private EditText etId;
    private Button button8;
    private String etIp;

    private LocalViewModel roomViewModel;
    private RecyclerView list;
    private List<LocalDeviceInfo> datas;
    private RoomAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        initView();

        if (SocketManager.getInstance().getUserState() == 1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar = findViewById(R.id.toolbar);
        etUser = findViewById(R.id.et_user);
        button8 = findViewById(R.id.button8);
        list = findViewById(R.id.list);
        refreshLayout = findViewById(R.id.swipe);
        etUser.setText(App.getInstance().getUsername());
        adapter = new RoomAdapter();
        datas = new ArrayList<>();
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        roomViewModel = new ViewModelProvider(this).get(LocalViewModel.class);
        refreshLayout.setOnRefreshListener(() -> roomViewModel.loadRooms());
        roomViewModel.getRoomList().observe(this, roomInfos -> {
            if (roomInfos.loadEnd()) {
                refreshLayout.setRefreshing(false);
            } else if (!datas.contains(roomInfos)) {
                datas.add(roomInfos);
                adapter.notifyDataSetChanged();
            }

        });
        // 添加登录回调
        SocketManager.getInstance().addUserStateCallback(this);
    }

    private static final String TAG = "LauncherActivity";

    public void updateUserName() {
        username = etUser.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "please input your name", Toast.LENGTH_LONG).show();
            return;
        }
        // 设置用户名
        App.getInstance().setUsername(username);
    }

    public void java(View view) {
        updateUserName();
        // 连接socket:登录
        SocketManager.getInstance().connect(Urls.WS, username, 0);
    }

    @Override
    public void localUserLogin(String userId, boolean isOutGoing) {
        Logger.i(TAG, "localUserLogin userId " + userId);
            CallSingleActivity.openActivity(this, userId, isOutGoing,
                    "NickName", false, false);

    }

    @Override
    public void userLogin(String userId) {
        Logger.i(TAG, "userLogin userId " + userId);
        startActivity(new Intent(this, MainActivity.class));

    }

    @Override
    public void userLogout() {

    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }
    }

    private Handler mHandler = new Handler();

    private class RoomAdapter extends RecyclerView.Adapter<Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rooms, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LocalDeviceInfo roomInfo = datas.get(position);
            holder.text.setText(roomInfo.getRoomId());
            holder.item_join_room.setOnClickListener(v -> {
                updateUserName();
                SocketManager.getInstance().connectLocal(AppRTCUtils.getIpAddress(), roomInfo.getUserId(), 0);
                UdpSocketUtils.get().sendRequest(roomInfo);
//                CallMultiActivity.openActivity(LauncherActivity.this, "room-921c2c70-256a-43"/*roomInfo.getRoomId()*/, false);
            });
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

    }

    private static class Holder extends RecyclerView.ViewHolder {

        private final TextView text;
        private final Button item_join_room;

        Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.item_user_name);
            item_join_room = itemView.findViewById(R.id.item_join_room);
        }
    }

    public void stop(View view) {
        SocketManager.getInstance().unConnect();
    }
}
