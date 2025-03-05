package com.mtech.gosafe;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatFragment extends Fragment {

    private EditText userQueryInput;
    private ImageView getPlanButton;
    private Button getDirectionButton;
    private TextView responseText;
    private String evacuationPlan;
    private RecyclerView recyclerView;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    public ChatFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userQueryInput = view.findViewById(R.id.userQueryInput);
        getPlanButton = view.findViewById(R.id.send_btn);
        getDirectionButton = view.findViewById(R.id.getDirectionButton);
        responseText = view.findViewById(R.id.responseText);
        recyclerView = view.findViewById(R.id.recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        getDirectionButton.setVisibility(View.GONE); // Hide initially

        getPlanButton.setOnClickListener(v -> fetchEvacuationPlan());
        getDirectionButton.setOnClickListener(v -> openMapFragment());
    }

    private void fetchEvacuationPlan() {
        String userQuery = userQueryInput.getText().toString().trim();
        if (userQuery.isEmpty()) {
            responseText.setText("Please enter your location and fire location.");
            return;
        }

        addToChat(userQuery, Message.SEND_BY_ME);
        userQueryInput.setText("");
        callAPI(userQuery);
    }

    void addToChat(String message, String sendBy) {
        getActivity().runOnUiThread(() -> {
            messageList.add(new Message(message, sendBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SEND_BY_BOT);
    }

    void callAPI(String question) {
        messageList.add(new Message("Typing...", Message.SEND_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("query", question);
            jsonBody.put("user_needs", "wheelchair accessible routes");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://e0bf-20-192-21-49.ngrok-free.app/get_plan")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        evacuationPlan = jsonObject.getString("evacuation_plan");
                        addResponse(evacuationPlan.trim());
                        getActivity().runOnUiThread(() -> getDirectionButton.setVisibility(View.VISIBLE));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body().toString());
                }
            }
        });
    }

    private void openMapFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("evacuationPlan", evacuationPlan);

        MapsFragment mapFragment = new MapsFragment();
        mapFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, mapFragment)
                .addToBackStack(null)
                .commit();
    }
}