package com.wearconnectivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.HeadlessJsTaskService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class WearConnectivityMessageClient implements MessageClient.OnMessageReceivedListener, LifecycleEventListener {

    public static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String TAG = "WearConnectivityMessageClient";
    private final MessageClient messageClient;
    private final ReactApplicationContext reactContext;
    private boolean isListenerAdded;

    public WearConnectivityMessageClient(ReactApplicationContext context) {
        this.reactContext = context;
        this.messageClient = Wearable.getMessageClient(context);
        messageClient.addListener(this);
        context.addLifecycleEventListener(this);
    }

    /**
     * Sends a message to the first nearby node among the provided connectedNodes.
     * If no nearby node is found, it invokes the error callback.
     */
    public void sendMessage(String path, ReadableMap messageData, List<Node> connectedNodes, Callback replyCb, Callback errorCb) {
        final List<String> messages = new ArrayList<>();
        boolean hasNearbyNode = false;
        final boolean[] hasError = {false};
        CountDownLatch countDownLatch = new CountDownLatch(connectedNodes.size());
        for (Node node : connectedNodes) {
            if (node.isNearby()) {
                hasNearbyNode = true;
                sendMessageToClient(path, messageData, node, args -> {
                    messages.add((String) args[0]);
                    countDownLatch.countDown();
                }, args -> {
                    hasError[0] = true;
                    messages.add((String) args[0]);
                    countDownLatch.countDown();
                });
            } else {
                countDownLatch.countDown();
            }
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            errorCb.invoke("exception when await countDownLatch: " + e);
            return;
        }

        // 等待所有回调完成再执行下面的代码
        if (hasNearbyNode) {
            String message = String.join("\n", messages);
            if (hasError[0]) {
                errorCb.invoke(message);
            } else {
                replyCb.invoke(message);
            }
        } else {
            errorCb.invoke("No nearby node found");
        }
    }

    /**
     * Called when a message is received.
     * Forwards the message to a HeadlessJs service.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        // 在 WearableListenerService 监听
        if (path.startsWith(START_ACTIVITY_PATH)) {
            return;
        }

        try {
            Intent service = new Intent(reactContext, WearConnectivityTask.class);
            String data = new String(messageEvent.getData());
            if (!TextUtils.isEmpty(data)) {
                JSONObject jsonObject = new JSONObject(data);
                WritableMap messageAsWritableMap = (WritableMap) JSONArguments.fromJSONObject(jsonObject);
                FLog.w(TAG, TAG + " onMessageReceived message: " + messageAsWritableMap);
                Bundle bundle = Arguments.toBundle(messageAsWritableMap);
                service.putExtras(bundle);
            }
            reactContext.startForegroundService(service);
            HeadlessJsTaskService.acquireWakeLockNow(reactContext);
        } catch (JSONException e) {
            FLog.w(TAG, TAG + " onMessageReceived with path: " + path + " failed with error: " + e);
        }
    }

    @Override
    public void onHostResume() {
        if (messageClient != null && !isListenerAdded) {
            Log.d(TAG, "Adding listener on host resume");
            messageClient.addListener(this);
            isListenerAdded = true;
        }
    }

    @Override
    public void onHostPause() {
        Log.d(TAG, "onHostPause: leaving listener active for background events");
    }

    @Override
    public void onHostDestroy() {
        if (messageClient != null && isListenerAdded) {
            Log.d(TAG, "Removing listener on host destroy");
            messageClient.removeListener(this);
            isListenerAdded = false;
        }
    }

    /**
     * Helper method that sends a message to a specific node.
     */
    private void sendMessageToClient(String path, ReadableMap messageData, Node node, Callback replyCb, Callback errorCb) {
        String nodeString = node.toString();
        OnSuccessListener<Object> onSuccessListener = object -> replyCb.invoke("message sent to client with node: " + nodeString + ", requestId: " + object.toString());
        OnFailureListener onFailureListener = error -> errorCb.invoke("message sending failed: " + error.toString());
        try {
            JSONObject messageJSON = new JSONObject(messageData.toHashMap());
            String payloadString = messageJSON.toString();
            String resultPath;
            byte[] resultPayload = null;
            if (TextUtils.isEmpty(path)) {
                resultPath = payloadString;
            } else {
                resultPath = path;
                resultPayload = payloadString.getBytes(StandardCharsets.UTF_8);
            }
            Task<Integer> sendTask = messageClient.sendMessage(node.getId(), resultPath, resultPayload);
//            Task<Integer> sendTask = messageClient.sendMessage(node.getId(), messageJSON.toString(), null);
            sendTask.addOnSuccessListener(onSuccessListener);
            sendTask.addOnFailureListener(onFailureListener);
        } catch (Exception e) {
            errorCb.invoke("sendMessage failed: " + e);
        }
    }
}