package com.mobicomkit.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.mobicomkit.MobiComKitConstants;
import com.mobicomkit.client.ui.R;
import com.mobicomkit.communication.message.Message;
import com.mobicomkit.user.MobiComUserPreference;
import com.mobicomkit.userinterface.MobiComActivity;
import com.mobicomkit.userinterface.MobiComConversationFragment;
import com.mobicomkit.userinterface.MobiComQuickConversationFragment;
import com.mobicomkit.instruction.InstructionUtil;
import net.mobitexter.mobiframework.commons.core.utils.ContactNumberUtils;
import net.mobitexter.mobiframework.json.GsonUtils;
import net.mobitexter.mobiframework.people.contact.Contact;
import net.mobitexter.mobiframework.people.contact.ContactUtils;

/**
 * Created by devashish on 4/2/15.
 */
public class MobiComKitBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MobiTexterBroadcastReceiver";

    private MobiComQuickConversationFragment quickConversationFragment;
    private MobiComConversationFragment conversationFragment;

    public MobiComKitBroadcastReceiver(MobiComQuickConversationFragment quickConversationFragment, MobiComConversationFragment conversationFragment) {
        this.quickConversationFragment = quickConversationFragment;
        this.conversationFragment = conversationFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Message message = null;
        String smsXmlString = intent.getStringExtra(MobiComKitConstants.MESSAGE_JSON_INTENT);
        if (!TextUtils.isEmpty(smsXmlString)) {
            message = (Message) GsonUtils.getObjectFromJson(smsXmlString, Message.class);
        }
        Log.i(TAG, "Received broadcast, action: " + action + ", sms: " + message);

        MobiComUserPreference userPreferences = MobiComUserPreference.getInstance(context);
        String formattedContactNumber = "";
        if (message != null && !conversationFragment.isBroadcastedToGroup(message.getBroadcastGroupId()) && !message.isSentToMany()) {
            quickConversationFragment.addMessage(message);
            formattedContactNumber = ContactNumberUtils.getPhoneNumber(message.getTo(), userPreferences.getCountryCode());
        } else if (message != null && message.isSentToMany() && BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            for (String toField : message.getTo().split(",")) {
                Message singleMessage = new Message(message);
                singleMessage.setBroadcastGroupId(null);
                singleMessage.setKeyString(message.getKeyString());
                singleMessage.setTo(toField);
                singleMessage.processContactIds(context);
                quickConversationFragment.addMessage(singleMessage);
            }
        }

        String keyString = intent.getStringExtra("keyString");

        if (BroadcastService.INTENT_ACTIONS.INSTRUCTION.toString().equals(action)) {
            InstructionUtil.showInstruction(context, intent.getIntExtra("resId", -1), intent.getBooleanExtra("actionable", false), R.color.instruction_color);
        } else if (BroadcastService.INTENT_ACTIONS.FIRST_TIME_SYNC_COMPLETE.toString().equals(action)) {
            quickConversationFragment.downloadConversations(true);
        } else if (BroadcastService.INTENT_ACTIONS.LOAD_MORE.toString().equals(action)) {
            quickConversationFragment.setLoadMore(intent.getBooleanExtra("loadMore", true));
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_SYNC_ACK_FROM_SERVER.toString().equals(action)) {
            if (formattedContactNumber.equals(conversationFragment.getFormattedContactNumber()) ||
                    conversationFragment.isBroadcastedToGroup(message.getBroadcastGroupId())) {
                conversationFragment.updateMessageKeyString(message);
            }
        } else if (BroadcastService.INTENT_ACTIONS.SYNC_MESSAGE.toString().equals(intent.getAction())) {
            if (formattedContactNumber.equals(conversationFragment.getFormattedContactNumber()) ||
                    conversationFragment.isBroadcastedToGroup(message.getBroadcastGroupId())) {
                conversationFragment.addMessage(message);
            }
            if (message.getBroadcastGroupId() == null) {
                quickConversationFragment.updateLastMessage(keyString, formattedContactNumber);
            }
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_MESSAGE.toString().equals(intent.getAction())) {
            formattedContactNumber = intent.getStringExtra("contactNumbers");
            if (PhoneNumberUtils.compare(formattedContactNumber, MobiComActivity.currentOpenedContactNumber)) {
                conversationFragment.deleteMessageFromDeviceList(keyString);
            } else {
                //Todo: if it is sent to many and remove from all.
                quickConversationFragment.updateLastMessage(keyString, formattedContactNumber);
            }
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_DELIVERY.toString().equals(action)) {
            if (formattedContactNumber.equals(conversationFragment.getFormattedContactNumber())) {
                conversationFragment.updateDeliveryStatus(message);
            }
        } else if (BroadcastService.INTENT_ACTIONS.DELETE_CONVERSATION.toString().equals(action)) {
            //Todo: If conversation fragment is visible then hide it.
            String contactNumber = intent.getStringExtra("contactNumber");
            Contact contact = ContactUtils.getContact(context, contactNumber);
            conversationFragment.clearList();
            quickConversationFragment.removeConversation(contact);
        } else if (BroadcastService.INTENT_ACTIONS.UPLOAD_ATTACHMENT_FAILED.toString().equals(action) && message != null) {
            conversationFragment.updateUploadFailedStatus(message);
        } else if (BroadcastService.INTENT_ACTIONS.MESSAGE_ATTACHMENT_DOWNLOAD_DONE.toString().equals(action) && message != null) {
            conversationFragment.updateDownloadStatus(message);
        }
    }
}
