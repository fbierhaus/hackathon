package com.verizon.mms.ui;

import com.verizon.mms.ui.VZMFragmentActivity.ActionMenuBuilder;
import com.verizon.mms.ui.widget.ActionItem;

public interface MenuSelectedListener {
    /*
     * Handle the menu and action menu items selection
     */
    boolean onActionItemSelected(ActionItem item);
    
    /*
     * Called when the menu has to be displayed
     */
    void onPrepareActionMenu(ActionMenuBuilder menu);
}
