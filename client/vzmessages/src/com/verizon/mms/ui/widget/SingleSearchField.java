package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class SingleSearchField extends EditText {

    private SingleSearchFieldListener mFieldListener;

    
    public SingleSearchField(Context context) {
        super(context);
        init();
    }

    public SingleSearchField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SingleSearchField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init(){
       this.addTextChangedListener(mTextWatcher);
    }

    public void setSingleSearchFieldListener(SingleSearchFieldListener listener) {
       mFieldListener = listener;
    }
    
    @Override
    protected void onFocusChanged(boolean focused, int direction,Rect previouslyFocusedRect) {
        if(mFieldListener != null){
            mFieldListener.onFocusChanged(focused);
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mFieldListener != null && keyCode == KeyEvent.KEYCODE_ENTER) {
            mFieldListener.onEnterKey();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (mFieldListener != null && event.getKeyCode() == KeyEvent.KEYCODE_DEL && getText().length() == 0) {
            
            	mFieldListener.onBackKey();
            
        	
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }
    
    private TextWatcher mTextWatcher = new TextWatcher(){
        @Override
        public void afterTextChanged(Editable s) {
            
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        	 if (mFieldListener != null){
                 mFieldListener.afterTextChanged();
             }
        }
        
    };
    

    public interface SingleSearchFieldListener {
        void onBackKey();
        void onEnterKey(); 
        void onFocusChanged(boolean hasFocus);
        void afterTextChanged();
    }
}

