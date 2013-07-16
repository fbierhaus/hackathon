/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Contact;

/**
 * This class displays the status for a single recipient of a message. It is used in the ListView of
 * DeliveryReportActivity.
 */
public class DeliveryReportListItem {
    private TextView mRecipientView;
    private TextView mStatusView;
    private TextView mRecipientNumberView;
    private ImageView mStatusIconView;
    private View mRow;
    private Typeface mTypeface;

	DeliveryReportListItem(View view, Typeface typeface) {
        mRow = view;
        mTypeface = typeface;
    }

    /**
     * Set the Value of the mRecipientView
     * 
     * @return the mRecipientView
     */
    public TextView getmRecipientView() {
        if (null == mRecipientView) {
            mRecipientView = (TextView) mRow.findViewById(R.id.delivery_report_receipent_name_textview);
            mRecipientView.setTypeface(mTypeface);
        }
        return mRecipientView;
    }

    /**
     * Set the Value of the mStatusView
     * 
     * @return the mStatusView
     */
    public TextView getmStatusView() {
        if (null == mStatusView) {
            mStatusView = (TextView) mRow.findViewById(R.id.delivery_status_textview);
            mRecipientView.setTypeface(mTypeface);
        }
        return mStatusView;
    }

    /**
     * Set the Value of the mRecipientNumberView
     * 
     * @return the mRecipientNumberView
     */
    public TextView getmRecipientNumberView() {
        if (null == mRecipientNumberView) {
            mRecipientNumberView = (TextView) mRow.findViewById(R.id.delivery_report_recipient_number_textview);
            mRecipientView.setTypeface(mTypeface);
        }
        return mRecipientNumberView;
    }

    /**
     * Set the Value of the mStatusIconView
     * 
     * @return the mStatusIconView
     */
    public ImageView getmStatusIconView() {
        if (null == mStatusIconView) {
            mStatusIconView = (ImageView) mRow.findViewById(R.id.delivery_status_icon_image);
        }
        return mStatusIconView;
    }

    public final void bind(DeliveryReportListItem view, Contact contact, String status, boolean failed, Activity activity) {
        if (view != null) {
            final TextView recip = view.getmRecipientView();
            final String name = contact.getName(false);
            
            if (name != null && name.length() != 0) {
            	recip.setVisibility(View.VISIBLE);
            	recip.setText(name);
            }
            else {
            	recip.setVisibility(View.GONE);
            }
            
            String number = contact.getNumber();
            if(!TextUtils.isEmpty(number)){
            	String numberType = activity.getString(R.string.deliverystatus_number_type, 
                        contact.getPrefix(), contact.getNumber());
                view.getmRecipientNumberView().setText(numberType);
            }
            else
            {
            	view.getmRecipientNumberView().setVisibility(View.GONE);
            }
            

            view.getmStatusView().setText(status);

            view.getmStatusIconView().setVisibility(failed ? View.VISIBLE : View.GONE);
        }
    }
}
