package com.verizon.mms.ui.widget;

import android.graphics.Bitmap;

public class ActionItem {
	private int iconId;
	private Bitmap thumb;
	private int title = 0;
	private int actionId = -1;
    private boolean selected;
    private boolean sticky;
    private String actionTitle;
    private Object tag;
    private String smileyCharacter;
	
    public ActionItem(int actionId, int title, int icon) {
        this.title = title;
        this.iconId = icon;
        this.actionId = actionId;
    }
    
    public ActionItem(int actionId, String title, int icon) {
        this.actionTitle = title;
        this.iconId = icon;
        this.actionId = actionId;
    }
    
    public ActionItem(int actionId, String title, int icon, String smileyText) {
        this.actionTitle = title;
        this.iconId = icon;
        this.actionId = actionId;
        this.smileyCharacter	= smileyText;
    }
    
	public void setTitle(int title) {
		this.title = title;
	}
	
	public int getTitle() {
		return this.title;
	}
	
	public void setActionTitle(String title) {
		this.actionTitle = title;
	}
	
	public String getActionTitle() {
		return this.actionTitle;
	}
	
	public void setSmileyCharacter(String ch) {
		this.smileyCharacter = ch;
	}
	
	public String getSmileyCharacter() {
		return this.smileyCharacter;
	}
	
	public void setIcon(int icon) {
		this.iconId = icon;
	}
	
	public int getIcon() {
		return this.iconId;
	}
    public void setActionId(int actionId) {
        this.actionId = actionId;
    }
    public int getActionId() {
        return actionId;
    }
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }
    public boolean isSticky() {
        return sticky;
    }
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	public boolean isSelected() {
		return this.selected;
	}
	public void setThumb(Bitmap thumb) {
		this.thumb = thumb;
	}
	public Bitmap getThumb() {
		return this.thumb;
	}
	public void setTag(Object tag){
		this.tag = tag;
	}
	public Object getTag(){
		return this.tag;
	}
}