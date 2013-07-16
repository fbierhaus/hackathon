package com.verizon.mms.util;

import java.io.Serializable;

public class RecentSearchItem implements Serializable{

    private static final long serialVersionUID = 4454507845139488946L;
    private String mLine1;
    private String mLine2;
    private byte[] mPlaceByteArray;
    private double mPOIDistance;
    
    public RecentSearchItem (String line1,String line2){
        mLine1=line1;
        mLine2=line2;
    }

    public void setPlaceByteArray(byte[] place){
        this.mPlaceByteArray=place;
    }

    public double getPOIDistance() {
        return mPOIDistance;
    }

    public void setPOIDistance(double distance) {
        this.mPOIDistance = distance;
    }

    public byte[] getPlaceByteArray(){
        return mPlaceByteArray;
    }
    
    public String getLine1(){
        return mLine1;
    }
    
    public String getLine2(){
        return mLine2;
    }
    
    public String toString(){
        String line1=getLine1();
        String line2=getLine2();
        if(line2!=null){
          return  line1 + "\n" + line2;
        }else{
          return line1;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RecentSearchItem)) {
            return false;
        }
        RecentSearchItem other = (RecentSearchItem) obj;
        if (mLine1 == null) {
            if (other.mLine1 != null) {
                return false;
            }
        } else if (!mLine1.equals(other.mLine1)) {
            return false;
        }
        if (mLine2 == null) {
            if (other.mLine2 != null) {
                return false;
            }
        } else if (!mLine2.equals(other.mLine2)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mLine1 == null) ? 0 : mLine1.hashCode());
        result = prime * result + ((mLine2 == null) ? 0 : mLine2.hashCode());
        return result;
    }
}
