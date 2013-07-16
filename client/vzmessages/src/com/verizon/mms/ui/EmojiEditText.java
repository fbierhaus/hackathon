package com.verizon.mms.ui;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.EmojiParser;

public class EmojiEditText extends EditText {

    private TextWatcher watcher;

    public EmojiEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EmojiEditText(Context c, AttributeSet a) {
        super(c, a);
    }

    public EmojiEditText(Context context) {
        super(context);
    }

    public boolean onTextContextMenuItem(int id) {
        boolean handled = super.onTextContextMenuItem(id);
        if (id == android.R.id.paste) {
            updateClipBoard();
        }
        return handled;
    }

    public void updateClipBoard() {
        removeTextChangedListener(watcher);
        CharSequence s = getText().toString().trim();
        Logger.debug("EMOJII before:" + s);
        CharSequence text;
        
        if (MmsConfig.enableEmojis) {
        	try {
        		text = EmojiParser.getInstance().addEmojiSpans(s, false);
        		setText(text);
        		setSelection(text.length());

        		if (Logger.IS_DEBUG_ENABLED) {
        			Logger.debug("EMOJII after:" + text);
        		}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        addTextChangedListener(watcher);
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        // TODO Auto-generated method stub
        this.watcher = watcher;
        super.addTextChangedListener(watcher);
    }

}
