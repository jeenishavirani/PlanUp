package com.example.planup;

import android.os.Bundle;
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

import com.example.planup.R;
import com.example.planup.adapter.ChatAdapter;
import com.example.planup.model.ChatMessage;
import com.example.planup.ai.IntentDetector;
import com.example.planup.ai.FollowUpQuestionGenerator;
import com.example.planup.ai.PlannerEngine;

import com.example.planup.adapter.ChatAdapter;
import com.example.planup.model.ChatMessage;


import java.util.ArrayList;
import java.util.List;

public class AIAssistantFragment extends Fragment {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageView btnSend;
    private LinearLayout chatInputLayout;
    private LinearLayout layoutEmptyChat;

    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private String currentIntent = null;
    private final List<String> collectedAnswers = new ArrayList<>();


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
        chatInputLayout = view.findViewById(R.id.chatInputLayout);
        layoutEmptyChat = view.findViewById(R.id.layoutEmptyChat);

        adapter = new ChatAdapter(messages);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        lm.setStackFromEnd(true); // chat behavior
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);

        updateEmptyState();

        // 🔹 SEND BUTTON
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;


            addMessage(text, ChatMessage.USER);
            etMessage.setText("");

            // 🧠 STEP 1: Detect intent
            if (currentIntent == null) {

                currentIntent = IntentDetector.detect(text);

                // Unknown intent handling
                if (currentIntent == null || currentIntent.equals("UNKNOWN")) {
                    addMessage(
                            "I can help you plan things like exams, daily routines, focus, health, or personal goals 😊\nTell me what you want help with.",
                            ChatMessage.AI
                    );
                    currentIntent = null;
                    return;
                }

                addMessage("Got it 👍 Let me ask you a few things.", ChatMessage.AI);

                List<String> questions =
                        FollowUpQuestionGenerator.getQuestions(currentIntent);

                if (questions == null || questions.isEmpty()) {
                    addMessage(
                            "I need a bit more detail. Can you explain your goal?",
                            ChatMessage.AI
                    );
                    currentIntent = null;
                    return;
                }

                addMessage(questions.get(0), ChatMessage.AI);
                return;
            }

            // 🧠 STEP 2: Collect answers
            collectedAnswers.add(text);

            List<String> questions =
                    FollowUpQuestionGenerator.getQuestions(currentIntent);

            if (questions != null && collectedAnswers.size() < questions.size()) {

                addMessage(
                        questions.get(collectedAnswers.size()),
                        ChatMessage.AI
                );

            } else {
                // 🧠 STEP 3: Generate final plan
                String plan =
                        PlannerEngine.generatePlan(currentIntent, collectedAnswers);

                addMessage(plan, ChatMessage.AI);

                // Reset state
                currentIntent = null;
                collectedAnswers.clear();
            }
        });

        // ✅ Keyboard handling
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(0, 0, 0, imeInsets.bottom);

            // User message
            messages.add(new ChatMessage(text, ChatMessage.USER));
            adapter.notifyItemInserted(messages.size() - 1);
            rvChat.scrollToPosition(messages.size() - 1);
            etMessage.setText("");

            updateEmptyState();

            // Fake AI reply (temporary)
            rvChat.postDelayed(() -> {
                messages.add(new ChatMessage(
                        "Got it 👍 I’ll help you plan this.",
                        ChatMessage.AI
                ));
                adapter.notifyItemInserted(messages.size() - 1);
                rvChat.scrollToPosition(messages.size() - 1);
            }, 600);
        });

        // ✅ CORRECT KEYBOARD + BOTTOM NAV HANDLING
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {

            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int bottomPadding = Math.max(imeInsets.bottom, navInsets.bottom);

            chatInputLayout.setPadding(
                    chatInputLayout.getPaddingLeft(),
                    chatInputLayout.getPaddingTop(),
                    chatInputLayout.getPaddingRight(),
                    bottomPadding
            );

            rvChat.setPadding(
                    rvChat.getPaddingLeft(),
                    rvChat.getPaddingTop(),
                    rvChat.getPaddingRight(),
                    bottomPadding + 8
            );


            return insets;
        });

        return view;
    }


    // 🔹 Helper to add messages safely
    private void addMessage(String text, int type) {
        messages.add(new ChatMessage(text, type));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        updateEmptyState();
    }

    // 🔹 Empty state
    // 🔹 EMPTY STATE HANDLING

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
