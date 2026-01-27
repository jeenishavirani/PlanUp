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

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageView btnSend;
    private LinearLayout chatHeader;
    private LinearLayout layoutEmptyChat;

    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String userName = "there";

    private enum State { IDLE, AWAITING_REMINDER, AWAITING_PRIORITY }
    private State currentState = State.IDLE;
    private List<GeminiTaskExtractor.TaskResult> pendingTasks = new ArrayList<>();
    private boolean pendingReminderPreference = false;

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

        if (messages.isEmpty()) {
            fetchUserNameAndGreet();
        }

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            addMessage(text, ChatMessage.USER);
            etMessage.setText("");

            handleConversation(text);
        });

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            chatHeader.setPadding(chatHeader.getPaddingLeft(), systemBars.top, chatHeader.getPaddingRight(), chatHeader.getPaddingBottom());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        return view;
    }

    private void handleConversation(String text) {
        switch (currentState) {
            case AWAITING_REMINDER:
                handleReminderResponse(text);
                break;
            case AWAITING_PRIORITY:
                handlePriorityResponse(text);
                break;
            case IDLE:
            default:
                processAIMessage(text);
                break;
        }
    }

    private void fetchUserNameAndGreet() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("fullName");
                            if (name == null || name.isEmpty()) {
                                name = documentSnapshot.getString("nickname");
                            }
                            
                            if (name != null && !name.isEmpty()) {
                                userName = name.split(" ")[0];
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
        GeminiTaskExtractor.extractTask(text, BuildConfig.GEMINI_API_KEY, new GeminiTaskExtractor.ExtractionCallback() {
            @Override
            public void onResult(GeminiTaskExtractor.ExtractionResult result) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    if (result.hasTasks && result.tasks != null && !result.tasks.isEmpty()) {
                        pendingTasks = result.tasks;
                        
                        // Check if reminder is already specified
                        boolean allHaveReminders = true;
                        for (GeminiTaskExtractor.TaskResult task : pendingTasks) {
                            if (!task.wantsReminder) {
                                allHaveReminders = false;
                                break;
                            }
                        }

                        if (allHaveReminders) {
                            pendingReminderPreference = true;
                            askForPriority();
                        } else {
                            currentState = State.AWAITING_REMINDER;
                            addMessage("Would you like me to set reminders for these tasks? (Yes/No)", ChatMessage.AI);
                        }
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

    private void handleReminderResponse(String text) {
        pendingReminderPreference = text.toLowerCase().contains("yes") || text.toLowerCase().contains("yeah") || text.toLowerCase().contains("sure");
        askForPriority();
    }

    private void askForPriority() {
        currentState = State.AWAITING_PRIORITY;
        addMessage("What priority level should I set for these tasks? (High, Medium, or Low)", ChatMessage.AI);
    }

    private void handlePriorityResponse(String text) {
        String priority = "Medium"; // Default
        String input = text.toLowerCase();
        if (input.contains("high")) priority = "High";
        else if (input.contains("low")) priority = "Low";
        else if (input.contains("medium")) priority = "Medium";

        saveAllPendingTasks(pendingReminderPreference, priority);
        
        currentState = State.IDLE;
        pendingTasks.clear();
        addMessage("Done! I've added your tasks with " + priority + " priority. ✅", ChatMessage.AI);
    }

    private void saveAllPendingTasks(boolean setReminder, String priority) {
        for (GeminiTaskExtractor.TaskResult taskResult : pendingTasks) {
            saveTaskToFirestore(taskResult, setReminder || taskResult.wantsReminder, priority);
        }
    }

    private void saveTaskToFirestore(GeminiTaskExtractor.TaskResult result, boolean alarm, String priority) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        TaskModel task = new TaskModel();
        task.setTitle(result.title);
        task.setDescription(result.description);
        task.setPriority(priority); // Use the user's chosen priority
        task.setCategory(result.category);
        task.setStatus("Pending");
        task.setAlarm(alarm);
        task.setCreatedAt(System.currentTimeMillis());

        Calendar cal = Calendar.getInstance();
        try {
            if (result.date != null && !result.date.equalsIgnoreCase("null") && !result.date.isEmpty()) {
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
            if (result.time != null && !result.time.equalsIgnoreCase("null") && !result.time.isEmpty()) {
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
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save task", e));
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
