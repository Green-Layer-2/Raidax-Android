package com.cloudcoin2.wallet.Utils;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient extends WebSocketListener {



    private static final int NORMAL_CLOSURE_STATUS = 1000;
    String mRequest;
    SocketListener listener;
    public WebSocketClient(String request, SocketListener SocketListener) {
        this.listener=SocketListener;
        this.mRequest=request;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send(ByteString.decodeHex(mRequest));
       // webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d("Receiving : " , text);
    }
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d("Receiving bytes  : " , bytes.hex());
        listener.onResponse(bytes.hex());
    }
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        Log.d("Closing : " , String.valueOf(code).concat("Reason").concat(reason));
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
        Log.d("Error : " , t.getMessage());
    }


}
