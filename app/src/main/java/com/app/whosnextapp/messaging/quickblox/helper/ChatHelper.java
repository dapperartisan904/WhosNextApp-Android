package com.app.whosnextapp.messaging.quickblox.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.app.whosnextapp.R;
import com.app.whosnextapp.apis.PostRequest;
import com.app.whosnextapp.apis.ResponseListener;
import com.app.whosnextapp.messaging.quickblox.listener.QBSystemMessageListeners;
import com.app.whosnextapp.messaging.quickblox.model.SampleConfigs;
import com.app.whosnextapp.messaging.quickblox.utils.ConstantEnum;
import com.app.whosnextapp.messaging.quickblox.utils.QbDialogHolder;
import com.app.whosnextapp.messaging.service.CallService;
import com.app.whosnextapp.model.DiscoverModel;
import com.app.whosnextapp.model.UserDetailModel;
import com.app.whosnextapp.utility.Constants;
import com.app.whosnextapp.utility.Globals;
import com.orhanobut.logger.Logger;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogCustomData;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.utils.DialogUtils;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import static com.app.whosnextapp.utility.Globals.getContext;
import static com.quickblox.users.QBUsers.signIn;

public class ChatHelper {

    @SuppressLint("StaticFieldLeak")
    private static ChatHelper instance;
    @SuppressLint("StaticFieldLeak")
    private static Globals globals = (Globals) getContext();
    private OnQBPublicGroupChatServicesListener onQBPublicGroupChatServicesListener;
    private ConstantEnum.QuickBloxDialogType quickBloxDialogType;
    private String groupName;
    private boolean isProcessing = false;
    private QBChatService qbChatService;
    private Activity activity;
    private OnQBChatServicesListener onQBChatServicesListener;
    private ChatMessageListener chatMessageListener;
    private Integer opponentId;
    private String dialogId;
    private boolean getAll;
    private QBSystemMessageListeners qbSystemMessageListeners;
    private Intent intentUpdateTotalUnreads = new Intent(Constants.WN_actionUpdateTotalUnread);
    private Intent intentUpdateDialogList = new Intent(Constants.WN_actionUpdateDialogs);
    private int totalDialogs;
    private ArrayList<QBChatDialog> qbDialogs = new ArrayList<>();

    private ChatHelper() {
        qbChatService = QBChatService.getInstance();
        qbChatService.setUseStreamManagement(true);
    }

    public static synchronized ChatHelper getInstance() {
        if (instance == null) {
            //QBSettings.getInstance().setLogLevel(LogLevel.DEBUG);
            QBChatService.setDebugEnabled(true);
            QBChatService.setConfigurationBuilder(buildChatConfigs());
            instance = new ChatHelper();
        }
        return instance;
    }

    static QBChatService.ConfigurationBuilder buildChatConfigs() {
        QBChatService.ConfigurationBuilder configurationBuilder = new QBChatService.ConfigurationBuilder();
        SampleConfigs qbConfigs = Globals.getSampleConfigs();
        configurationBuilder.setPort(qbConfigs.getChatPort());
        configurationBuilder.setSocketTimeout(qbConfigs.getChatSocketTimeout()); //Sets chat socket's read timeout in seconds
        configurationBuilder.setKeepAlive(qbConfigs.isKeepAlive()); //Sets connection socket's keepAlive option.
        configurationBuilder.setAutojoinEnabled(qbConfigs.isAutoJoinEnabled());
        configurationBuilder.setAutoMarkDelivered(qbConfigs.isAutoMarkDelivered());
        configurationBuilder.setUseTls(qbConfigs.isUseTls()); //Sets the TLS security mode used when making the connection. By default TLS is disabled.
        QBChatService.setConfigurationBuilder(configurationBuilder);
        return configurationBuilder;
    }

    private static QBChatMessage createChatNotificationForGroupChatCreation(QBChatDialog dialog) {
        String dialogId = String.valueOf(dialog.getDialogId());
        QBChatMessage chatMessage = new QBChatMessage();
        // Add notification_type=1 to extra params when you created a group chat
//        chatMessage.setProperty("notification_type", "1");
        chatMessage.setProperty("_id", dialogId);
        return chatMessage;
    }

    private static QBChatDialog prepareDialog(Integer opponentId) {
        List<QBUser> users = new ArrayList<>();
        QBUser qbUser = new QBUser();
        qbUser.setId(opponentId);
        users.add(qbUser);
        return DialogUtils.buildDialog(users.toArray(new QBUser[users.size()]));
    }

    public static QBUser getCurrentUser() {
        return QBChatService.getInstance().getUser();
    }

    public static void setUpQBUser(Activity activity, UserDetailModel.Status userDetails, OnQBCreateUserListener qbCreateUserListener) {
        QBUser qbUser = new QBUser();
        qbUser.setFullName(userDetails.getName());
        qbUser.setLogin(userDetails.getUserName());
        qbUser.setPassword(Constants.WN_QB_PASSWORD);
        qbUser.setEmail(userDetails.getEmail());
        registerNupdateToQB(activity, userDetails.getCustomerId(), qbUser, qbCreateUserListener);
    }

    private static void registerNupdateToQB(final Activity activity, final String UserId, final QBUser qbUser, final OnQBCreateUserListener qbCreateUserListener) {
        ChatHelper.getInstance().signUpQBUser(qbUser, new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                doRequestForUpdateQBId(activity, UserId, qbUser.getId(), qbCreateUserListener);
            }

            @Override
            public void onError(QBResponseException e) {
                if (e.getHttpStatusCode() == Constants.WN_ERR_QB_LOGIN_TAKEN) {
                    ChatHelper.getInstance().getExistingQBUser(qbUser, new QBEntityCallback<QBUser>() {
                        @Override
                        public void onSuccess(QBUser user, Bundle bundle) {
                            doRequestForUpdateQBId(activity, UserId, user.getId(), qbCreateUserListener);
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            qbCreateUserListener.onQBCreateUserFailed();
                        }
                    });
                } else
                    qbCreateUserListener.onQBCreateUserFailed();
            }
        });
    }

    private static void doRequestForUpdateQBId(final Activity activity, String UserId, final Integer qbUserId, final OnQBCreateUserListener qbCreateUserListener) {
        String requestURL = String.format(activity.getString(R.string.get_update_chatId_by_customerId), qbUserId, globals.getUserDetails().getStatus().getCustomerId());
        new PostRequest(activity, null, requestURL, true, true, new ResponseListener() {
            @Override
            public void onSucceedToPostCall(String response) {
                if (qbCreateUserListener != null)
                    qbCreateUserListener.onQBCreateUserSuccess(qbUserId);
            }

            @Override
            public void onFailedToPostCall(int statusCode, String msg) {
                if (qbCreateUserListener != null)
                    qbCreateUserListener.onQBCreateUserFailed();
            }
        }).execute();
    }

    public void logoutFromChat(Context context) {
        if (qbChatService == null)
            return;
        QbDialogHolder.getInstance().clearDialogs();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nMgr != null;
        nMgr.cancelAll();
        try {
            qbChatService.logout(new QBEntityCallback() {

                @Override
                public void onSuccess(Object o, Bundle bundle) {
                    qbChatService.destroy();
                }

                @Override
                public void onError(QBResponseException errors) {

                }
            });
        } catch (Exception ignored) {

        }
    }

    public boolean isLogged() {
        return QBChatService.getInstance().isLoggedIn();
    }

    private QBUser getQBUser() {
        UserDetailModel userDetailModel = globals.getUserDetails();
        QBUser qbUser = new QBUser();
        qbUser.setLogin(userDetailModel.getStatus().getUserName());
        qbUser.setFullName(userDetailModel.getStatus().getName());
        qbUser.setPassword(Constants.WN_QB_PASSWORD);
        qbUser.setEmail(userDetailModel.getStatus().getEmail());
        return qbUser;
    }

    public void init(Activity activity, String dialogId, ConstantEnum.QuickBloxDialogType quickBloxDialogType, boolean getAll, OnQBChatServicesListener onQBChatServicesListener, boolean isProcessing) {
        this.activity = activity;
        this.dialogId = dialogId;
        this.getAll = getAll;

        qbSystemMessageListeners = new QBSystemMessageListeners();
        this.onQBChatServicesListener = onQBChatServicesListener;
        chatMessageListener = new ChatMessageListener();
        if (this.isProcessing)
            return;
        this.isProcessing = isProcessing;
        if (isLogged()) {
            retriveDialogs();
        } else {
            signInUser(getQBUser(), true, quickBloxDialogType);
        }
    }

    private void signInUser(final QBUser qbuser, final boolean fromDialogs, final ConstantEnum.QuickBloxDialogType quickBloxDialogType) {
        if (qbChatService == null) {
            qbChatService = QBChatService.getInstance();
        }
        signInQBUser(qbuser, new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                loginToChat(qbuser, fromDialogs, quickBloxDialogType);
            }

            @Override
            public void onError(QBResponseException e) {
                if (e.getHttpStatusCode() == 401)
                    registerQuickBloxUser(qbuser, fromDialogs, quickBloxDialogType);
                else {
                    onQBChatServicesListener.onServiceProcessFailed(e.getMessage());
                    isProcessing = false;
                }
            }
        });
    }

    private void registerQuickBloxUser(final QBUser qbUser, final boolean fronDialogs, final ConstantEnum.QuickBloxDialogType quickBloxDialogType) {
        signUpQBUser(qbUser, new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser user, Bundle bundle) {
                qbUser.setId(user.getId());
                UserDetailModel userDetailModel = globals.getUserDetails();
                globals.setUserDetails(userDetailModel);
                doRequestForUpdateQBId(activity, globals.getUserDetails().getStatus().getCustomerId(), qbUser.getId(), null);
                signInUser(qbUser, fronDialogs, quickBloxDialogType);
            }

            @Override
            public void onError(QBResponseException e) {
                if (e.getErrors() != null && !e.getErrors().isEmpty()) {
                    if (e.getHttpStatusCode() == Constants.WN_ERR_QB_LOGIN_TAKEN) {
                        getExistingQBUser(qbUser, new QBEntityCallback<QBUser>() {
                            @Override
                            public void onSuccess(QBUser qbUser, Bundle bundle) {
                                qbUser.setId(qbUser.getId());
                                qbUser.setPassword(Constants.WN_QB_PASSWORD);
                                signInUser(qbUser, fronDialogs, quickBloxDialogType);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                onQBChatServicesListener.onServiceProcessFailed(e.getMessage());
                                isProcessing = false;
                            }
                        });
                    } else {
                        onQBChatServicesListener.onServiceProcessFailed(e.getMessage());
                        isProcessing = false;
                    }
                }
            }
        });
    }

    private void startLoginService(QBUser qbUser) {
        Intent tempIntent = new Intent(activity, CallService.class);
        PendingIntent pendingIntent = activity.createPendingResult(Constants.EXTRA_LOGIN_RESULT_CODE, tempIntent, 0);
        CallService.start(activity, qbUser, pendingIntent);
    }

    private void loginToChat(final QBUser user, final boolean fromDialogs, final ConstantEnum.QuickBloxDialogType quickBloxDialogType) {
        if (qbChatService == null) {
            return;
        }
        chatLogin(user, new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                qbSystemMessageListeners.listen(quickBloxDialogType);
                if (fromDialogs)
                    retriveDialogs();
                else if (quickBloxDialogType == ConstantEnum.QuickBloxDialogType.private_chat)
                    createDialogWithSelectedUsers(activity, opponentId, onQBChatServicesListener);
                else if (quickBloxDialogType == ConstantEnum.QuickBloxDialogType.public_group)
                    createSelectedPublicGroup(activity, groupName, onQBPublicGroupChatServicesListener);
                startLoginService(user);
            }

            @Override
            public void onError(QBResponseException e) {
                if (e.getMessage() != null && e.getMessage().equalsIgnoreCase(Constants.WN_ERR_QB_LOGGED_IN)) {
                    retriveDialogs();
                } else
                    loginToChat(user, fromDialogs, quickBloxDialogType);
            }
        });
    }

    void signUpQBUser(final QBUser qbUser, QBEntityCallback<QBUser> callback) {
        QBUsers.signUp(qbUser).performAsync(callback);
    }

    private void signInQBUser(final QBUser qbUser, QBEntityCallback<QBUser> callback) {
        signIn(qbUser).performAsync(callback);
    }

    void getExistingQBUser(final QBUser qbUser, QBEntityCallback<QBUser> callback) {
        QBUsers.getUserByLogin(qbUser.getLogin()).performAsync(callback);
    }

    private void chatLogin(final QBUser qbUser, QBEntityCallback<QBUser> callback) {
        qbChatService.login(qbUser, callback);
    }

    public void getUsersFromDialog(QBChatDialog dialog,
                                   final QBEntityCallback<ArrayList<QBUser>> callback) {
        List<Integer> userIds = dialog.getOccupants();
        QBPagedRequestBuilder requestBuilder = new QBPagedRequestBuilder(userIds.size(), 1);
        QBUsers.getUsersByIDs(userIds, requestBuilder).performAsync(callback);
    }

    private void retriveDialogs() {
        if (getAll)
            getChats();
        else
            getDialogById(dialogId, quickBloxDialogType);
    }

    private void getChats() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                // code goes here
                QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
                requestBuilder.setLimit(100);
                if (qbDialogs.size() > 0)
                    requestBuilder.setSkip(qbDialogs.size());
                QBRestChatService.getChatDialogs(null, requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
                    @Override
                    public void onSuccess(ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {
                        if (qbChatDialogs.size() > 0) {
                            totalDialogs = bundle.getInt(Constants.WN_TOTAL_ENTRIES);
                            qbDialogs.addAll(qbChatDialogs);
                            Logger.e(String.valueOf(totalDialogs) + " : " + qbDialogs.size());
                            if (qbDialogs.size() >= totalDialogs)
                                setUpFinalDialogList(qbDialogs, ConstantEnum.QuickBloxDialogType.all);
                            else
                                getChats();
                        } else {
                            setUpFinalDialogList(qbDialogs, ConstantEnum.QuickBloxDialogType.all);
                        }
                    }

                    @Override
                    public void onError(QBResponseException errors) {
                        onQBChatServicesListener.onServiceProcessFailed(errors.getMessage());
                        isProcessing = false;
                    }
                });
            }
        });
    }

    private String getRecipient(QBChatDialog qbChatDialog) {
        if (globals.getUserDetails() == null)
            return "0";
        for (Integer id : qbChatDialog.getOccupants()) {
            if (!String.valueOf(id).equalsIgnoreCase(String.valueOf(globals.getUserDetails().getStatus().getChatId())))
                return String.valueOf(id);
        }
        return String.valueOf(qbChatDialog.getRecipientId());
    }

    private JSONArray prepareQBIds(ArrayList<QBChatDialog> qbChatDialogs) {
        ArrayList<String> userIds = new ArrayList<>();
        for (QBChatDialog qbChatDialog : qbChatDialogs) {
            userIds.add(getRecipient(qbChatDialog));
        }
        return new JSONArray(userIds);
    }

    private void setUpFinalDialogList(ArrayList<QBChatDialog> qbChatDialogs, ConstantEnum.QuickBloxDialogType quickBloxDialogType) {
        if (chatMessageListener == null)
            chatMessageListener = new ChatMessageListener();
        for (QBChatDialog qbChatDialog : qbChatDialogs) {
            if (qbChatDialog != null && QBChatService.getInstance() != null) {
                try {
                    if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                        qbChatDialog.initForChat(QBChatService.getInstance());
                        if (!QbDialogHolder.getInstance().dialogPresent(qbChatDialog.getDialogId(), true)) {
                            qbChatDialog.addMessageListener(chatMessageListener);
                        }
                        QbDialogHolder.getInstance().addDialog(qbChatDialog, true);
                    } else if (qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP) {

                        qbChatDialog.initForChat(QBChatService.getInstance());
                        if (!QbDialogHolder.getInstance().dialogPresent(qbChatDialog.getDialogId(), false)) {
                            qbChatDialog.addMessageListener(chatMessageListener);
                        }


                        QbDialogHolder.getInstance().addDialog(qbChatDialog, false);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intentUpdateTotalUnreads);
        isProcessing = false;
        if (quickBloxDialogType == ConstantEnum.QuickBloxDialogType.public_group && onQBPublicGroupChatServicesListener != null)
            onQBPublicGroupChatServicesListener.onPublicGroupProcessSuccess(qbChatDialogs);
        else if (onQBChatServicesListener != null)
            onQBChatServicesListener.onServiceProcessSuccess(qbChatDialogs);
        else
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intentUpdateDialogList);
        clearTemps();
    }

    private void clearTemps() {
        totalDialogs = 0;
        qbDialogs.clear();
    }

    public void getDialogById(String dialogId, final ConstantEnum.QuickBloxDialogType quickBloxDialogType) {
        QBRestChatService.getChatDialogById(dialogId).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                ArrayList<QBChatDialog> qbChatDialogs = new ArrayList<QBChatDialog>();
                qbChatDialogs.add(qbChatDialog);
                setUpFinalDialogList(qbChatDialogs, quickBloxDialogType);
            }

            @Override
            public void onError(QBResponseException e) {
                if (onQBChatServicesListener != null)
                    onQBChatServicesListener.onServiceProcessFailed(e.getMessage());
            }
        });
    }

    private void createDialog(Integer opponentId, QBEntityCallback<QBChatDialog> callback) {
        QBRestChatService.createChatDialog(prepareDialog(opponentId)).performAsync(callback);
    }

    public void createDialogWithSelectedUsers(Activity activity, final Integer opponentId, final OnQBChatServicesListener onQBChatServicesListener) {
        this.activity = activity;
        this.opponentId = opponentId;
        this.onQBChatServicesListener = onQBChatServicesListener;
        chatMessageListener = new ChatMessageListener();
        qbSystemMessageListeners = new QBSystemMessageListeners();

        if (isLogged()) {
            createDialog(opponentId, new QBEntityCallback<QBChatDialog>() {
                @Override
                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                    Logger.e(String.valueOf(qbChatDialog));
                    ArrayList<QBChatDialog> qbChatDialogs = new ArrayList<QBChatDialog>();
                    qbChatDialogs.add(qbChatDialog);
                    setUpFinalDialogList(qbChatDialogs, ConstantEnum.QuickBloxDialogType.private_chat);
                    notifyCreation(qbChatDialog, opponentId);
                }

                @Override
                public void onError(QBResponseException e) {
                    onQBChatServicesListener.onServiceProcessFailed(e.getMessage());
                }
            });
        } else {
            signInUser(getQBUser(), false, ConstantEnum.QuickBloxDialogType.private_chat);
        }
    }

    private void createPublicGroup(String name, QBEntityCallback<QBChatDialog> callback) {
        QBChatDialog dialog = new QBChatDialog();
        dialog.setName(name);
        dialog.setType(QBDialogType.PUBLIC_GROUP);
        QBRestChatService.createChatDialog(dialog).performAsync(callback);
    }

    private void createSelectedPublicGroup(Activity activity, String name, final OnQBPublicGroupChatServicesListener onQBPublicGroupChatServicesListener) {
        this.activity = activity;
        this.groupName = name;
        this.onQBPublicGroupChatServicesListener = onQBPublicGroupChatServicesListener;
        chatMessageListener = new ChatMessageListener();
        qbSystemMessageListeners = new QBSystemMessageListeners();

        if (isLogged()) {
            createPublicGroup(name, new QBEntityCallback<QBChatDialog>() {
                @Override
                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                    Logger.e(String.valueOf(qbChatDialog));
                    ArrayList<QBChatDialog> qbChatDialogs = new ArrayList<>();
                    qbChatDialogs.add(qbChatDialog);
                    setUpFinalDialogList(qbChatDialogs, ConstantEnum.QuickBloxDialogType.public_group);
                }

                @Override
                public void onError(QBResponseException e) {
                    onQBPublicGroupChatServicesListener.onPublicGroupProcessFailed(e.getMessage());
                }
            });
        } else {
            signInUser(getQBUser(), false, ConstantEnum.QuickBloxDialogType.private_chat);
        }
    }

    private void notifyCreation(QBChatDialog qbChatDialog, Integer opponentId) {
        QBSystemMessagesManager systemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
        QBChatMessage chatMessage = createChatNotificationForGroupChatCreation(qbChatDialog);
        chatMessage.setRecipientId(opponentId);
        try {
            systemMessagesManager.sendSystemMessage(chatMessage);
        } catch (SmackException.NotConnectedException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private DiscoverModel.CustomerList getUser(ArrayList<DiscoverModel.CustomerList> customerData, String recipientId) {
        if (customerData != null && customerData.size() > 0) {
            for (DiscoverModel.CustomerList userDetails : customerData) {
                if (userDetails.getChatId().equals(recipientId))
                    return userDetails;
            }
        }
        return null;
    }

    public ArrayList<QBChatDialog> setUpIndividualDialogList(ArrayList<DiscoverModel.CustomerList> customerData, ArrayList<QBChatDialog> qbChatDialogs) {
        ArrayList<QBChatDialog> qbArrayList = new ArrayList<>();
        ArrayList<DiscoverModel.CustomerList> customerLists = new ArrayList<>();
        for (QBChatDialog qbChatDialog : qbChatDialogs) {
            if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                DiscoverModel.CustomerList userDetails = getUser(customerData, getRecipient(qbChatDialog));
                if (userDetails != null) {
                    qbChatDialog.setName(userDetails.getFirstName() + " " + userDetails.getLastName());
                    qbChatDialog.setPhoto(userDetails.getProfilePicUrl());
                    QBDialogCustomData qbDialogCustomData = new QBDialogCustomData();
                    qbDialogCustomData.putString(Constants.WN_USERNAME, userDetails.getUserName());
                    qbDialogCustomData.putInteger(Constants.WN_CUSTOMER_ID, userDetails.getCustomerId());
                    qbDialogCustomData.putInteger(Constants.WN_CHAT_ID, Integer.parseInt(userDetails.getChatId()));
                    qbDialogCustomData.putString(Constants.WN_NAME, userDetails.getFirstName() + userDetails.getLastName());
                    qbChatDialog.setCustomData(qbDialogCustomData);
                    qbArrayList.add(qbChatDialog);
                    customerLists.add(userDetails);
                }
            }
        }

        if (customerData.size() > qbChatDialogs.size()) {
            customerData.removeAll(customerLists);
            for (int i = 0; i < customerData.size(); i++) {
                DiscoverModel.CustomerList userDetails = customerData.get(i);
                QBChatDialog qbChatDialog = new QBChatDialog();
                qbChatDialog.setName(userDetails.getFirstName() + " " + userDetails.getLastName());
                qbChatDialog.setPhoto(userDetails.getProfilePicUrl());
                QBDialogCustomData qbDialogCustomData = new QBDialogCustomData();
                qbDialogCustomData.putString(Constants.WN_USERNAME, userDetails.getUserName());
                qbDialogCustomData.putInteger(Constants.WN_CUSTOMER_ID, userDetails.getCustomerId());
                qbDialogCustomData.putInteger(Constants.WN_CHAT_ID, Integer.parseInt(userDetails.getChatId()));
                qbDialogCustomData.putString(Constants.WN_NAME, userDetails.getFirstName() + userDetails.getLastName());
                qbDialogCustomData.putString(Constants.WN_QuickBloxId, String.valueOf(userDetails.getChatId()));
                qbChatDialog.setCustomData(qbDialogCustomData);
                qbArrayList.add(qbChatDialog);
            }
        }
        return qbArrayList;
    }

    public void getDialogMessage(QBChatDialog qbChatDialog, final boolean forReferesh, long dateSent, final OnQBChatMeesageListener onQBChatMeesageListener) {
        QBRequestGetBuilder customObjectRequestBuilder = new QBRequestGetBuilder();
        customObjectRequestBuilder.setLimit(Constants.WN_MessageCount);
        if (forReferesh)
            customObjectRequestBuilder.lt("date_sent", dateSent);
        customObjectRequestBuilder.sortDesc("date_sent");
        QBRestChatService.getDialogMessages(qbChatDialog, customObjectRequestBuilder)
                .performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                    @Override
                    public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                        onQBChatMeesageListener.messageReSuccess(qbChatMessages);
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        onQBChatMeesageListener.messageReFailed(e.getMessage());
                    }
                });
    }

    public interface OnQBChatServicesListener {
        void onServiceProcessSuccess(ArrayList<QBChatDialog> qbChatDialogs);

        void onServiceProcessFailed(String errorMessage);
    }

    public interface OnQBPublicGroupChatServicesListener {
        void onPublicGroupProcessSuccess(ArrayList<QBChatDialog> qbChatDialogs);

        void onPublicGroupProcessFailed(String errorMessage);
    }

    public interface OnQBChatMeesageListener {
        void messageReSuccess(ArrayList<QBChatMessage> qbChatMessages);

        void messageReFailed(String message);
    }

    public interface OnQBCreateUserListener {
        void onQBCreateUserSuccess(int QuickBloxId);

        void onQBCreateUserFailed();
    }
}

