package com.example.socketservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SocketService extends Service {

    private Socket socket; // 소켓 객체

    private String RcvData; // 수신 데이터

    private PrintWriter pw; // 출력 스트림
    private BufferedReader br; // 입력 스트림

    public String getRcvData() {
        return this.RcvData;
    }

    public void setRcvData(String RcvData) {
        this.RcvData = RcvData;
    }

    // Binder 클래스를 상속받은 LocalBinder 클래스 정의
    public class LocalBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }

    // Binder 객체 생성
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // 스레드 풀 생성
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    // 서버에 연결하는 메소드
    public Future<Void> connect(String ip, int port) {
    // executorService.submit 메서드를 호출하여 비동기 작업을 제출
    // 이 메서드는 Callable 객체를 매개변수로 받아들입니다. Callable은 Java에서 결과를 반환하는 작업을 나타내는 인터페이스
    // 여기서는 람다 표현식을 사용하여 Callable 인터페이스의 인스턴스를 만든다
    return executorService.submit(() -> {
        // 소켓 객체를 생성한다. 이는 IP 주소와 포트 번호를 매개변수로 받아들인다
        // 이 소켓은 클라이언트 소켓으로, 이를 사용하여 서버와의 연결을 설정한다
        socket = new Socket(ip, port);

        // Callable은 결과를 반환해야 하므로, 이 작업의 결과는 null
        // 이는 실제로 이 작업이 소켓을 만드는 작업이며, 이 작업의 "결과"는 실제로 작업이 성공적으로 완료되었는지 여부에 관계없이 null
        return null;
    });
}


public Future<Void> disconnect() {
    // executorService.submit 메서드를 호출하여 비동기 작업을 제출합니다.
    // 여기서는 람다 표현식을 사용하여 Callable 인터페이스의 인스턴스를 만듭니다.
    return executorService.submit(() -> {
        // 이미 열려있는 소켓을 닫습니다. 이는 네트워크 자원을 정리하는데 필요합니다.
        socket.close();
        // Callable은 결과를 반환해야 하므로, 이 작업의 결과는 null입니다.
        return null;
    });
}

public Future<String> receive() {
    // executorService.submit 메서드를 호출하여 비동기 작업을 제출합니다.
    return executorService.submit(() -> {
        // 소켓에서 데이터를 읽기 위한 BufferedReader를 생성합니다.
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // 소켓에서 입력 스트림을 얻습니다.
        InputStream inputStream = socket.getInputStream();
        byte[] byteRcv = new byte[512];
        // 입력 스트림에서 데이터를 읽습니다.
        int readByteCount = inputStream.read(byteRcv);
        // 읽은 데이터를 문자열로 변환하고 저장합니다.
        setRcvData(new String(byteRcv, 0, readByteCount, "UTF-8"));
        // 로그 메시지를 출력합니다.
        Log.d("SocketHost", "rcvThread: " + getRcvData());
        // 수신한 데이터를 반환합니다.
        return getRcvData();
    });
}

public Future<Void> send(String message) {
    // executorService.submit 메서드를 호출하여 비동기 작업을 제출합니다.
    return executorService.submit(() -> {
        // 소켓에 데이터를 쓰기 위한 PrintWriter를 생성합니다.
        pw = new PrintWriter(socket.getOutputStream());
        // 메시지를 소켓에 씁니다.
        pw.println(message.toString() + '\0');
        // PrintWriter의 버퍼를 비웁니다. 이는 모든 보류 중인 출력이 실제로 소켓에 쓰여지도록 보장합니다.
        pw.flush();
        // Callable은 결과를 반환해야 하므로, 이 작업의 결과는 null입니다.
        return null;
    });
}
}
