package com.verizon.mms.ui;

import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.verizon.mms.ui.widget.OnOffTabHost;
import com.verizon.mms.ui.widget.OnOffTabHost.OnTabChangedListener;

public class EmojiPageChangeListener implements OnPageChangeListener, OnTabChangedListener {
    private OnOffTabHost mTabHost;
    private ViewPager mViewPager;

    public EmojiPageChangeListener(OnOffTabHost mt, ViewPager viewPager) {
        this.mTabHost = mt;
        this.mViewPager = viewPager;
    }

    public void onPageSelected(int position) {
        this.mTabHost.setSelected(position);
    }
 
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    public void onPageScrollStateChanged(int arg0) {

    }

	@Override
	public void onTabChanged(int position) {
		this.mViewPager.setCurrentItem(position);
	}
}
