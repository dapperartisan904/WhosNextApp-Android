package com.app.whosnextapp.messaging.videocall.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import com.app.whosnextapp.R;
import com.app.whosnextapp.messaging.BaseToolBarFragment;
import com.app.whosnextapp.messaging.videocall.interfaces.ConversationFragmentCallbackListener;
import com.app.whosnextapp.messaging.videocall.utils.CollectionsUtils;
import com.app.whosnextapp.utility.Constants;
import com.app.whosnextapp.utility.WebRtcSessionManager;
import com.quickblox.chat.QBChatService;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCSession;

import java.util.ArrayList;

public abstract class BaseConversationFragment extends BaseToolBarFragment implements CallActivity.CurrentCallStateCallback {

    protected WebRtcSessionManager sessionManager;
    protected QBRTCSession currentSession;
    protected ArrayList<QBUser> opponents;
    protected ConversationFragmentCallbackListener conversationFragmentCallbackListener;
    protected Chronometer timerChronometer;
    protected boolean isStarted;
    protected View outgoingOpponentsRelativeLayout;
    protected TextView allOpponentsTextView;
    protected TextView ringingTextView;
    protected QBUser currentUser;
    private boolean isIncomingCall;
    private ToggleButton micToggleVideoCall;
    private ImageButton handUpVideoCall;

    public static BaseConversationFragment newInstance(BaseConversationFragment baseConversationFragment, boolean isIncomingCall) {
        Bundle args = new Bundle();
        args.putBoolean(Constants.EXTRA_IS_INCOMING_CALL, isIncomingCall);

        baseConversationFragment.setArguments(args);

        return baseConversationFragment;
    }

    private static QBUser createStubUserById(Integer userId) {
        QBUser stubUser = new QBUser(userId);
        stubUser.setFullName(String.valueOf(userId));
        return stubUser;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            conversationFragmentCallbackListener = (ConversationFragmentCallbackListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ConversationFragmentCallbackListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversationFragmentCallbackListener.addCurrentCallStateCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        sessionManager = WebRtcSessionManager.getInstance(getActivity());
        currentSession = sessionManager.getCurrentSession();
        if (currentSession == null) {
            return view;
        }
        initFields();
        initViews(view);
        initButtonsListener();
        prepareAndShowOutgoingScreen();
        return view;
    }

    private void prepareAndShowOutgoingScreen() {
        configureOutgoingScreen();
        allOpponentsTextView.setText(CollectionsUtils.makeStringFromUsersFullNames(opponents));
    }

    protected abstract void configureOutgoingScreen();

    protected void initFields() {
        currentUser = QBChatService.getInstance().getUser();
        sessionManager = WebRtcSessionManager.getInstance(getActivity());
        currentSession = sessionManager.getCurrentSession();

        if (getArguments() != null) {
            isIncomingCall = getArguments().getBoolean(Constants.EXTRA_IS_INCOMING_CALL);
        }
        initOpponentsList();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (currentSession == null) {
            return;

        }
        if (currentSession.getState() != QBRTCSession.QBRTCSessionState.QB_RTC_SESSION_CONNECTED) {
            if (isIncomingCall) {
                currentSession.acceptCall(null);
            } else {
                currentSession.startCall(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        conversationFragmentCallbackListener.removeCurrentCallStateCallback(this);
        super.onDestroy();
    }

    protected void initViews(View view) {
        micToggleVideoCall = (ToggleButton) view.findViewById(R.id.toggle_mic);
        handUpVideoCall = (ImageButton) view.findViewById(R.id.button_hangup_call);
        outgoingOpponentsRelativeLayout = view.findViewById(R.id.layout_background_outgoing_screen);
        allOpponentsTextView = (TextView) view.findViewById(R.id.text_outgoing_opponents_names);
        ringingTextView = (TextView) view.findViewById(R.id.text_ringing);

        if (isIncomingCall) {
            hideOutgoingScreen();
        }
    }

    protected void initButtonsListener() {

        micToggleVideoCall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                conversationFragmentCallbackListener.onSetAudioEnabled(isChecked);
            }
        });

        handUpVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtonsEnabled(false);
                handUpVideoCall.setEnabled(false);
                handUpVideoCall.setActivated(false);

                conversationFragmentCallbackListener.onHangUpCurrentSession();
            }
        });
    }

    protected void actionButtonsEnabled(boolean inability) {

        micToggleVideoCall.setEnabled(inability);

        micToggleVideoCall.setActivated(inability);
    }

    private void startTimer() {
        if (!isStarted) {
            timerChronometer.setVisibility(View.VISIBLE);
            timerChronometer.setBase(SystemClock.elapsedRealtime());
            timerChronometer.start();
            isStarted = true;
        }
    }

    private void stopTimer() {
        if (timerChronometer != null) {
            timerChronometer.stop();
            isStarted = false;
        }
    }

    private void hideOutgoingScreen() {
        outgoingOpponentsRelativeLayout.setVisibility(View.GONE);
    }

    @Override
    public void onCallStarted() {
        hideOutgoingScreen();
        startTimer();
        actionButtonsEnabled(true);
    }

    @Override
    public void onCallStopped() {
        if (currentSession == null) {
            return;
        }
        stopTimer();
        actionButtonsEnabled(false);
    }

    @Override
    public void onOpponentsListUpdated(ArrayList<QBUser> newUsers) {
        initOpponentsList();
    }

    private void initOpponentsList() {
        if (opponents == null)
            opponents = new ArrayList<>();
        for (int i = 0; i < currentSession.getOpponents().size(); i++) {
            opponents.add(createStubUserById(currentSession.getOpponents().get(i)));
        }
        QBUser caller = new QBUser(currentSession.getCallerID());
        caller.setFullName(String.valueOf(currentSession.getCallerID()));

        if (isIncomingCall) {
            opponents.add(caller);
            opponents.remove(QBChatService.getInstance().getUser());
        }
    }
}