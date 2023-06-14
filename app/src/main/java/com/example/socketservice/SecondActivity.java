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

public class SecondActivity extends AppCompatActivity {

    private SocketService socketService; // SocketService 객체 선언
    private boolean isBound = false; // 서비스 연결 여부를 나타내는 변수
    EditText messageEditText; // 메시지 입력을 위한 EditText
    TextView receivedTextView; // 수신된 메시지를 보여주는 TextView

    // 서비스 연결을 위한 ServiceConnection 객체 생성
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SocketService.LocalBinder binder = (SocketService.LocalBinder) service; // LocalBinder 객체 생성
            socketService = binder.getService(); // SocketService 객체 가져오기
            isBound = true; // 서비스 연결 상태를 true로 변경
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false; // 서비스 연결 상태를 false로 변경
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // SocketService와 연결하기 위한 Intent 생성
        Intent intent = new Intent(this, SocketService.class);
        // Service와 연결
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // 레이아웃에서 View 객체 참조
        messageEditText = findViewById(R.id.messageEditText);
        receivedTextView = findViewById(R.id.receivedTextView);
        Button sendButton = findViewById(R.id.sendButton);

        // 전송 버튼 클릭 시 실행될 코드 작성
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 새로운 Thread 생성
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // sendAndReceive() 메소드 실행
                        sendAndReceive();
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    private void sendAndReceive() {
        if (isBound) {
            executorService.submit(() -> {
                try {
                    // 메시지 입력란에서 문자열을 가져온다.
                    String message = String.valueOf(messageEditText.getText());
                    // 소켓 서비스를 사용하여 메시지를 보낸다.
                    // get() 메서드를 사용하여 작업이 완료될 때까지 대기한다.
                    socketService.send(message).get();
                    // 소켓 서비스를 사용하여 메시지를 수신한다.
                    // get() 메서드를 사용하여 작업이 완료될 때까지 대기한다.
                    String received = socketService.receive().get();
                    // UI 스레드에서 수신된 메시지를 표시한다.
                    runOnUiThread(() -> receivedTextView.setText(received));
                } catch (InterruptedException | ExecutionException e) {
                    // 예외가 발생하면 런타임 예외를 던진다.
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void send() throws IOException {
        // 메시지 변수에 "message" 문자열을 할당합니다.
        String message = "message";
        // 소켓 서비스 객체의 send 메소드를 호출하여 메시지를 전송합니다.
        socketService.send(message);
    }

}
