package com.verizon.mms.ui;

import java.util.ArrayList;

public class GalleryRowData {
    long rowId;
    int count;
    int totalWeight;
    ArrayList<GalleryItem> items = new ArrayList<GalleryItem>(3);


    public GalleryRowData(long rowId) {
        this.rowId = rowId;
    }

    public void add(GalleryItem item) {
        if (item != null) {
        	items.add(item);
            count++;
            totalWeight += item.weight;
        }
    }

    public GalleryItem getItem(int position) {
    	if (position >= 0 && position < count) {
    		return items.get(position);
    	}
    	return null;
    }

    @Override
    public String toString() {
    	return super.toString() + ": id = " + rowId + ", weight = " + totalWeight + ", items = " + items;
    }
}
