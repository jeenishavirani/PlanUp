package com.example.planup;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planup.adapter.ChatAdapter;
import com.example.planup.model.ChatMessage;
import com.example.planup.model.TaskModel;
import com.example.planup.ai.GeminiTaskExtractor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AIAssistantFragment extends Fragment {

    private static final String TAG = "AIAssistantFragment";
    private static final String GEMINI_API_KEY = "AIzaSyDcttbdMngaa7Sm1xk2recRrlldoLW-r7o";

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageView btnSend;
    private LinearLayout chatHeader;
    private LinearLayout layoutEmptyChat;

    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String userName = "there";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        rvChat = view.findViewById(R.id.rvChat);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        layoutEmptyChat = view.findViewById(R.id.layoutEmptyChat);
        chatHeader = view.findViewById(R.id.chatHeader);

        adapter = new ChatAdapter(messages);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);

        // Only fetch and greet if the chat is empty (start of session)
        if (messages.isEmpty()) {
            fetchUserNameAndGreet();
        }

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            addMessage(text, ChatMessage.USER);
            etMessage.setText("");

            processAIMessage(text);
        });

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            chatHeader.setPadding(chatHeader.getPaddingLeft(), systemBars.top, chatHeader.getPaddingRight(), chatHeader.getPaddingBottom());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        return view;
    }

    private void fetchUserNameAndGreet() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Changed to "fullName" as used in SignUpActivity
                            String name = documentSnapshot.getString("fullName");
                            if (name == null || name.isEmpty()) {
                                name = documentSnapshot.getString("nickname");
                            }
                            
                            if (name != null && !name.isEmpty()) {
                                userName = name.split(" ")[0]; // Get first name
                            }
                        }
                        addGreeting();
                    })
                    .addOnFailureListener(e -> addGreeting());
        } else {
            addGreeting();
        }
    }

    private void addGreeting() {
        if (messages.isEmpty()) {
            addMessage("Hi " + userName + "! 👋 I'm your PlanUp assistant. How can I help you today?", ChatMessage.AI);
        }
    }

    private void processAIMessage(String text) {
        GeminiTaskExtractor.extractTask(text, GEMINI_API_KEY, new GeminiTaskExtractor.ExtractionCallback() {
            @Override
            public void onResult(GeminiTaskExtractor.TaskResult result) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (result.isTask) {
                        saveTaskToFirestore(result);
                    } else {
                        addMessage(result.reply != null ? result.reply : "I'm here to help!", ChatMessage.AI);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> 
                    addMessage("I'm sorry: " + error + " 🌐", ChatMessage.AI)
                );
            }
        });
    }

    private void saveTaskToFirestore(GeminiTaskExtractor.TaskResult result) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        TaskModel task = new TaskModel();
        task.setTitle(result.title);
        task.setStatus("Pending");
        task.setCreatedAt(System.currentTimeMillis());

        Calendar cal = Calendar.getInstance();
        try {
            if (result.date != null && !result.date.equalsIgnoreCase("null")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date d = sdf.parse(result.date);
                if (d != null) {
                    Calendar dCal = Calendar.getInstance();
                    dCal.setTime(d);
                    cal.set(Calendar.YEAR, dCal.get(Calendar.YEAR));
                    cal.set(Calendar.MONTH, dCal.get(Calendar.MONTH));
                    cal.set(Calendar.DAY_OF_MONTH, dCal.get(Calendar.DAY_OF_MONTH));
                }
            }
            if (result.time != null && !result.time.equalsIgnoreCase("null")) {
                String[] t = result.time.split(":");
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
                cal.set(Calendar.MINUTE, Integer.parseInt(t[1]));
            }
            task.setDueDate(cal.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Parsing error", e);
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("tasks").add(task)
                .addOnSuccessListener(ref -> addMessage(result.reply != null ? result.reply : "Task added: " + result.title + " ✅", ChatMessage.AI))
                .addOnFailureListener(e -> addMessage("Failed to save task to Firestore.", ChatMessage.AI));
    }

    private void addMessage(String text, int type) {
        messages.add(new ChatMessage(text, type));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (messages.isEmpty()) {
            layoutEmptyChat.setVisibility(View.VISIBLE);
            rvChat.setVisibility(View.GONE);
        } else {
            layoutEmptyChat.setVisibility(View.GONE);
            rvChat.setVisibility(View.VISIBLE);
        }
    }
}
