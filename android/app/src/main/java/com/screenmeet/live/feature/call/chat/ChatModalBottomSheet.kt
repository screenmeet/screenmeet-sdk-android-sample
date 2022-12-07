package com.screenmeet.live.feature.call.chat

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.screenmeet.live.R
import com.screenmeet.live.databinding.FragmentChatBinding
import com.screenmeet.sdk.ScreenMeet
import com.screenmeet.sdk.SessionEventListener
import com.screenmeet.sdk.domain.entity.ChatMessage

class ChatModalBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentChatBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = object : BottomSheetDialog(requireContext(), theme) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                findViewById<View>(com.google.android.material.R.id.container)?.apply {
                    fitsSystemWindows = false
                }
                findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
                    false
            }
        }

        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            val layoutParams = bottomSheet!!.layoutParams
            val windowHeight = Resources.getSystem().displayMetrics.heightPixels - 400
            if (layoutParams != null) layoutParams.height = windowHeight
            bottomSheet.layoutParams = layoutParams
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.peekHeight = -1
            behavior.skipCollapsed = true
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendMessageBtn.setOnClickListener { _: View? ->
            val text = binding.chatEt.text.toString().trim { it <= ' ' }
            if (text.isNotEmpty()) {
                ScreenMeet.sendChatMessage(text)
                binding.chatEt.text.clear()
            }
        }
        displayMessages()
    }

    private fun displayMessages() {
        binding.chatRecycler.apply {
            val linearLayoutManager = LinearLayoutManager(binding.root.context)
            linearLayoutManager.stackFromEnd = true
            val chatAdapter = ChatAdapter()
            layoutManager = linearLayoutManager
            adapter = chatAdapter
            val chatMessages = ScreenMeet.getChatMessages()
            chatAdapter.submitList(chatMessages) { smoothScrollToPosition(chatMessages.size) }
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenMeet.registerEventListener(eventListener)
    }

    override fun onPause() {
        super.onPause()
        ScreenMeet.unregisterEventListener(eventListener)
    }

    private var eventListener: SessionEventListener = object : SessionEventListener {
        override fun onChatMessage(chatMessage: ChatMessage) {
            binding.chatRecycler.apply {
                val chatAdapter = adapter
                if (chatAdapter is ChatAdapter) {
                    val chatMessages = ScreenMeet.getChatMessages()
                    chatAdapter.submitList(chatMessages) { smoothScrollToPosition(chatMessages.size) }
                }
            }
        }
    }
}
