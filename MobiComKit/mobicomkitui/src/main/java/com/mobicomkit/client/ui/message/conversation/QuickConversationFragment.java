package com.mobicomkit.client.ui.message.conversation;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.mobicomkit.client.ui.MobiComKitApplication;
import com.mobicomkit.client.ui.R;
import com.mobicomkit.client.ui.message.MessageIntentService;
import com.mobicomkit.userinterface.ConversationAdapter;
import com.mobicomkit.userinterface.MobiComQuickConversationFragment;

public class QuickConversationFragment extends MobiComQuickConversationFragment {
;
    public QuickConversationFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conversationService = new ConversationService(getActivity());
        conversationAdapter = new ConversationAdapter(getActivity(),
                R.layout.mobicom_message_row_view, messageList, null, true, MessageIntentService.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(MobiComKitApplication.TITLE);
    }
}

