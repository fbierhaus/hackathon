package com.rocketmobile.widget;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.MmsConfig;

/**
 * Home screen widget
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Apr 29, 2012
 */
public class HomeScreenWidget extends AppWidgetProvider {

    private static int          messageCount          = 0;

    public static String        MESSAGE_COUNT_CHANGED = "com.verizon.widget.MESSAGE_COUNT_CHANGED";
    public static String        UNREAD_MESSAGE_COUNT  = "com.verizon.widget.UNREAD_MESSAGE_COUNT";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            int widgetId = appWidgetIds[i];
            RemoteViews views = null;
            /*if (OEM.isSAMSUNGGalaxytab) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_galaxytab);
            } else*/ 
            if (MmsConfig.isTabletDevice()) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_galaxytab);
            } else if (OEM.isSAMSUNGAegis) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_samsung_aegis);
            } else if (OEM.isSAMSUNG) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_samsung);
            } else if (OEM.isMOTOROLARazr) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_samsung_aegis);
            } else if (OEM.isMOTOROLADroidX2) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_motorola);
            } else if (OEM.isLGRevolution) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_revolution);
            } else if (OEM.isLGRevolution2) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_revolution2);
            } else if (OEM.isMOTOROLADroid3) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_droid3);
            } else if (OEM.isMOTOROLADroid2 || OEM.isMOTOROLADroidGlobal) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_droid2);
            } else if (OEM.isHTCDNA || OEM.isHTCRezound || OEM.isHTCInc3) {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_htcdna);
            } else {
                views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            }

            if (messageCount == 0) {
                views.setViewVisibility(R.id.unreadMessagesCount, View.INVISIBLE);
            } else if (messageCount > 99) {
                views.setTextViewText(R.id.unreadMessagesCount, "99+");
                views.setViewVisibility(R.id.unreadMessagesCount, View.VISIBLE);
            } else {
                views.setTextViewText(R.id.unreadMessagesCount, "" + messageCount);
                views.setViewVisibility(R.id.unreadMessagesCount, View.VISIBLE);
            }

            // If clicked, launch WigetLaunchActivity which will eventually launch ConversationListActivity
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(context, WidgetLaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setType("vnd.android-dir/mms-sms");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_shortcut_image, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (MESSAGE_COUNT_CHANGED.equals(intent.getAction())) {
            // get count
            messageCount = (null != intent.getExtras()) ? intent.getExtras().getInt(UNREAD_MESSAGE_COUNT) : 0;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                    HomeScreenWidget.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
        // else, start service!
        else if ("android.appwidget.action.APPWIDGET_UPDATE".equalsIgnoreCase(intent.getAction())) {
            context.startService(new Intent(context, WidgetNotificationService.class));
        }
    }

    @Override
    public void onEnabled(Context context) {
        context.startService(new Intent(context, WidgetNotificationService.class));
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        context.stopService(new Intent(context, WidgetNotificationService.class));
        super.onDisabled(context);
    }

    public static void updateWidget(Context context, int count) {
        Intent intent = new Intent(com.rocketmobile.widget.HomeScreenWidget.MESSAGE_COUNT_CHANGED);
        intent.putExtra(UNREAD_MESSAGE_COUNT, count);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (CanceledException e) {
            Logger.error(e);
        }
    }

}
