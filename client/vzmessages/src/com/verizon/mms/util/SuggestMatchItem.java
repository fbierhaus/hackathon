package com.verizon.mms.util;

import com.nbi.search.singlesearch.SuggestMatch;

public class SuggestMatchItem {

    private SuggestMatch mNBISuggestMatch;
    
    public SuggestMatchItem (SuggestMatch suggest){
        mNBISuggestMatch = suggest;
    }
    
    public String getLine1(){
        return mNBISuggestMatch.getLine1();
    }
    
    public String getLine2(){
        return mNBISuggestMatch.getLine2();
    }
    
    public String toString(){
        String line1=getLine1();
        String line2=getLine2();
        if(line2!=null){
          return  line1 + " " + line2;
        }else{
          return line1;
        }
    }
}
