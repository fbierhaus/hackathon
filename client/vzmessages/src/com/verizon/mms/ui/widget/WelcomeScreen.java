package com.verizon.mms.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.verizon.messaging.vzmsgs.R;

public class WelcomeScreen {
	private Activity activity;
	private Point p, p1;
	private LayoutInflater layoutInflater;
	private View layout;
	PopupWindow popup;
	private boolean isMultipaneUI;
	private boolean mIsLandscape;
	private boolean isOpened;

	public WelcomeScreen(Activity activity, boolean isMultipaneUI,
			boolean mIsLandscape, Point p, Point p1) {
		this.activity = activity;
		this.isMultipaneUI = isMultipaneUI;
		this.mIsLandscape = mIsLandscape;
		this.p = p;
		this.p1 = p1;
		this.isOpened = false;
		layoutInflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout = layoutInflater.inflate(R.layout.popup_welcome_screen, null);
	}

	private void loadXY() {

		if (p == null) {
			int[] location = new int[2];
			ImageViewButton composeButton = (ImageViewButton) activity
					.findViewById(R.id.composebutton);
			composeButton.getLocationOnScreen(location);
			p = new Point();
			p.x = location[0];
			p.y = location[1];
		}
		if (p1 == null) {
			int[] location1 = new int[2];
			ImageViewButton imgButton = (ImageViewButton) activity
					.findViewById(R.id.imgGallery);
			imgButton.getLocationOnScreen(location1);
			p1 = new Point();
			p1.x = location1[0];
			p1.y = location1[1];
		}

		if (isMultipaneUI) {
			layout = getTabletLandWC();
		} else {
			if (mIsLandscape) {
				layout = getHandsetLandWC();
			} else {
				layout = getHandsetPortWC();
			}
		}
	}

	private View getHandsetPortWC() {
		ImageViewButton composeButton = (ImageViewButton) activity
				.findViewById(R.id.composebutton);
		ImageViewButton imgButton = (ImageViewButton) activity
				.findViewById(R.id.imgGallery);

		ImageViewButton overlayCompBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayCompBtn);
		RelativeLayout.LayoutParams rl;

		rl = new RelativeLayout.LayoutParams(composeButton.getWidth(),
				composeButton.getHeight());
		rl.leftMargin = p.x - 10;
		rl.topMargin = p.y;
		rl.rightMargin = p.x + composeButton.getWidth() - 10;
		rl.bottomMargin = p.y + composeButton.getHeight();
		overlayCompBtn.setLayoutParams(rl);

		rl = null;
		ImageViewButton overlayImgBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayImgBtn);
		rl = new RelativeLayout.LayoutParams(imgButton.getWidth(),
				imgButton.getHeight());
		rl.leftMargin = p1.x - 10;
		rl.topMargin = p.y;
		rl.width = imgButton.getWidth();
		rl.height = composeButton.getHeight();
		rl.rightMargin = p1.x + imgButton.getWidth() - 10;
		rl.bottomMargin = p1.y + imgButton.getHeight();
		overlayImgBtn.setLayoutParams(rl);

		layout.findViewById(R.id.tab_msg_tp1).setVisibility(View.GONE);
		ImageView tip1 = (ImageView) layout.findViewById(R.id.msg_tp1);
		tip1.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = p1.y - 3;
		rl.rightMargin = (activity.getWindowManager().getDefaultDisplay()
				.getWidth()
				- p1.x - 10);
		rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tip1.setLayoutParams(rl);

		layout.findViewById(R.id.msg_tp2_land).setVisibility(View.GONE);
		layout.findViewById(R.id.tab_msg_tip2).setVisibility(View.GONE);
		ImageView tip21 = (ImageView) layout.findViewById(R.id.msg_tp2);
		tip21.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = p.y + composeButton.getHeight();
		rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tip21.setLayoutParams(rl);

		layout.findViewById(R.id.tab_msg_tip3).setVisibility(View.GONE);
		ImageView tip3 = (ImageView) layout.findViewById(R.id.msg_tip3);
		tip3.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
		rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		tip3.setLayoutParams(rl);
		return layout;

	}

	private View getHandsetLandWC() {
		ImageViewButton composeButton = (ImageViewButton) activity
				.findViewById(R.id.composebutton);
		ImageViewButton imgButton = (ImageViewButton) activity
				.findViewById(R.id.imgGallery);

		ImageViewButton overlayCompBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayCompBtn);
		overlayCompBtn.setVisibility(View.VISIBLE);
		RelativeLayout.LayoutParams rl;

		rl = new RelativeLayout.LayoutParams(composeButton.getWidth(),
				composeButton.getHeight());
		rl.leftMargin = p.x - 10;
		rl.topMargin = p.y;
		rl.rightMargin = p.x + composeButton.getWidth() - 10;
		rl.bottomMargin = p.y + composeButton.getHeight();
		// rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		overlayCompBtn.setLayoutParams(rl);

		rl = null;
		ImageViewButton overlayImgBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayImgBtn);
		rl = new RelativeLayout.LayoutParams(imgButton.getWidth(),
				imgButton.getHeight());
		rl.leftMargin = p1.x - 10;
		rl.topMargin = p.y;
		rl.width = imgButton.getWidth();
		rl.height = composeButton.getHeight();
		rl.rightMargin = p1.x + imgButton.getWidth() - 10;
		rl.bottomMargin = p1.y + imgButton.getHeight();
		overlayImgBtn.setLayoutParams(rl);

		layout.findViewById(R.id.tab_msg_tp1).setVisibility(View.GONE);
		ImageView tip1 = (ImageView) layout.findViewById(R.id.msg_tp1);
		tip1.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = p1.y - 3;
		rl.rightMargin = (activity.getWindowManager().getDefaultDisplay()
				.getWidth()
				- p1.x - 10);
		rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tip1.setLayoutParams(rl);

		layout.findViewById(R.id.msg_tp2).setVisibility(View.GONE);
		layout.findViewById(R.id.tab_msg_tip2).setVisibility(View.GONE);
		ImageView tip21 = (ImageView) layout.findViewById(R.id.msg_tp2_land);
		tip21.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = p.y + composeButton.getHeight();
		rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tip21.setLayoutParams(rl);

		layout.findViewById(R.id.tab_msg_tip3).setVisibility(View.GONE);
		ImageView tip3 = (ImageView) layout.findViewById(R.id.msg_tip3);
		tip3.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		tip3.setLayoutParams(rl);
		return layout;
	}

	private View getTabletLandWC() {
		ImageViewButton composeButton = (ImageViewButton) activity
				.findViewById(R.id.composebutton);
		ImageViewButton imgButton = (ImageViewButton) activity
				.findViewById(R.id.imgGallery);

		ImageViewButton overlayCompBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayCompBtn);
		overlayCompBtn.setVisibility(View.VISIBLE);
		RelativeLayout.LayoutParams rl;

		rl = new RelativeLayout.LayoutParams(composeButton.getWidth(),
				composeButton.getHeight());
		rl.leftMargin = p.x - 10;
		rl.topMargin = p.y;
		rl.rightMargin = p.x + composeButton.getWidth() - 10;
		rl.bottomMargin = p.y + composeButton.getHeight();
		overlayCompBtn.setLayoutParams(rl);

		rl = null;
		ImageViewButton overlayImgBtn = (ImageViewButton) layout
				.findViewById(R.id.overlayImgBtn);
		rl = new RelativeLayout.LayoutParams(imgButton.getWidth(),
				imgButton.getHeight());
		rl.leftMargin = p1.x - 10;
		rl.topMargin = p.y;
		rl.width = imgButton.getWidth();
		rl.height = composeButton.getHeight();
		rl.rightMargin = p1.x + imgButton.getWidth() - 10;
		rl.bottomMargin = p1.y + imgButton.getHeight();
		overlayImgBtn.setLayoutParams(rl);

		layout.findViewById(R.id.msg_tp1).setVisibility(View.GONE);
		ImageView tip1 = (ImageView) layout.findViewById(R.id.tab_msg_tp1);
		tip1.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = imgButton.getHeight() - 15;
		rl.rightMargin = (activity.getWindowManager().getDefaultDisplay()
				.getWidth() - p1.x) - 45;
		rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		tip1.setLayoutParams(rl);

		layout.findViewById(R.id.msg_tp2).setVisibility(View.GONE);
		layout.findViewById(R.id.msg_tp2_land).setVisibility(View.GONE);
		ImageView tab_msg_tip2 = (ImageView) layout
				.findViewById(R.id.tab_msg_tip2);
		tab_msg_tip2.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.topMargin = composeButton.getHeight();
		rl.leftMargin = p.x + 5;
		rl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		tab_msg_tip2.setLayoutParams(rl);

		layout.findViewById(R.id.msg_tip3).setVisibility(View.GONE);
		ImageView tip3 = (ImageView) layout.findViewById(R.id.tab_msg_tip3);
		tip3.setVisibility(View.VISIBLE);
		rl = null;
		rl = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.leftMargin = 40;
		rl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		tip3.setLayoutParams(rl);
		return layout;
	}

	public void open() {
		dismiss();
		popup = new PopupWindow((Context) activity);
		popup.setContentView(layout);
		popup.setWidth(activity.getWindowManager().getDefaultDisplay()
				.getWidth());
		popup.setHeight(activity.getWindowManager().getDefaultDisplay()
				.getHeight());
		popup.setFocusable(true);
		popup.setTouchable(true);
		popup.setBackgroundDrawable(activity.getResources().getDrawable(
				R.drawable.popupbg));
		popup.setWindowLayoutMode(ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);
		isOpened = true;
		popup.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				isOpened = false;
				if (popup != null)
					popup.dismiss();
				return true;
			}
		});
		popup.showAtLocation(layout, Gravity.NO_GRAVITY, 0, 0);
	}

	public boolean ismIsLandscape() {
		return this.mIsLandscape;
	}

	public boolean isShown() {
		return isOpened;
	}

	public Point getP() {
		return p;
	}

	public void setP(Point p) {
		this.p = p;
	}

	public Point getP1() {
		return p1;
	}

	public void setP1(Point p1) {
		this.p1 = p1;
	}

	public void show() {
		loadXY();
		open();
	}

	public void dismiss() {
		if (popup != null) {
			isOpened = false;
			popup.dismiss();
			popup = null;
		}
	}

}
