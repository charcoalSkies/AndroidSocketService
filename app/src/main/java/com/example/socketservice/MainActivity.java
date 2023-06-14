package com.example.socketservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private SocketService socketService; // SocketService 객체 선언
    private boolean isBound = false; // 서비스가 바인드되었는지 여부를 나타내는 변수
    EditText messageEditText; // 메시지 입력을 위한 EditText
    TextView receivedTextView; // 수신된 메시지를 보여주는 TextView

    private ServiceConnection connection = new ServiceConnection() { // 서비스 연결을 위한 ServiceConnection 객체 생성
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) { // 서비스가 연결되었을 때 호출되는 메소드
            SocketService.LocalBinder binder = (SocketService.LocalBinder) service; // IBinder 객체를
                                                                                    // SocketService.LocalBinder 객체로 캐스팅
            socketService = binder.getService(); // SocketService 객체 가져오기
            isBound = true; // 서비스가 바인드되었음을 표시
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) { // 서비스가 연결이 끊겼을 때 호출되는 메소드
            isBound = false; // 서비스가 바인드되지 않았음을 표시
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SocketService와 연결하기 위한 Intent 생성
        Intent intent = new Intent(this, SocketService.class);
        // SocketService와 연결
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // 버튼과 EditText, TextView 객체 생성
        Button sendButton = findViewById(R.id.sendButton);
        Button connectButton = findViewById(R.id.connectButton);
        Button nextButton = findViewById(R.id.nextButton);
        messageEditText = findViewById(R.id.messageEditText);
        receivedTextView = findViewById(R.id.receivedTextView);

        // sendButton 클릭 시 sendAndReceive() 메소드 실행
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAndReceive();
            }
        });

        // nextButton 클릭 시 SecondActivity로 이동
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                        // intent.putExtra("message", messageEditText.getText().toString()); // 페이지 전환시 데이터 넘길일이 있다면 사용
                        startActivity(intent); // SecondActivity로 이동
                    }
                }).start(); // 새로운 스레드에서 실행
            }
        });

        // connectButton 클릭 시 SocketService의 connect() 메소드 실행
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socketService.connect("10.0.2.2", 8080);
            }
        });
    }

    // 스레드 풀 생성
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void sendAndReceive() {
        if (isBound) {
            // 스레드 풀에서 작업 실행
            executorService.submit(() -> {
                try {
                    // EditText에서 메시지를 가져와 SocketService의 send() 메소드 실행
                    String message = String.valueOf(messageEditText.getText()); // messageEditText에서 텍스트를 가져와서 String으로 변환 후 message 변수에 할당
                    socketService.send(message).get(); // 소켓 서비스를 통해 메시지를 전송하고, 전송 완료까지 대기한다.
                    
                    // SocketService의 receive() 메소드 실행하여 받은 메시지를 TextView에 출력
                    String received = socketService.receive().get(); // 소켓 서비스로부터 데이터를 받아와서 received 변수에 저장한다.
                    runOnUiThread(() -> receivedTextView.setText(received)); // UI 스레드에서 실행되도록 함. receivedTextView에 received 값을 설정하여 화면에 출력함.
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            // SocketService와 연결 해제
            unbindService(connection);
            isBound = false;
        }
    }
}
