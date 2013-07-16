package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.Vma.LinkedVMADevices;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;

public class ManageDeviceList extends VZMActivity {
    private ListView mDeviceTable;
    private TextView noDeviceFound;
    private DeviceListAdapter mAdapter;
    private Button mRemoveAll;
    private List<LinkedVMADevices> mDeviceList;
    private Context context;
    private RelativeLayout bottomLayout;
    private ProvisionManager provMng;
    private ProgressBar mProgresBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        setContentView(R.layout.manage_devices_list);
        context = this;
        provMng = new ProvisionManager(context);
        mDeviceList = new ArrayList<LinkedVMADevices>();
        mProgresBar = (ProgressBar) findViewById(R.id.progressBar);
        getDeviceList();
        mDeviceTable = (ListView) findViewById(R.id.device_list);
        noDeviceFound = (TextView) findViewById(R.id.textView1);
        bottomLayout = (RelativeLayout)findViewById(R.id.bottomLayout);
        bottomLayout.setVisibility(View.GONE);
        mAdapter = new DeviceListAdapter();
        mDeviceTable.setAdapter(mAdapter);
        mRemoveAll = (Button) findViewById(R.id.removeAll);
        mRemoveAll.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                deleteAllDevice();
            }
        });
        super.onCreate(savedInstanceState);
    }

    protected void deleteAllDevice() {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null, getString(R.string.vma_removing_all_devices));
            }

            @Override
            protected Integer doInBackground(Void... params) {

                return provMng.deleteAllLinkedDevices();
            };

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    mDeviceList.clear();
                    mAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context, R.string.vma_not_remove_conn_problem, Toast.LENGTH_LONG).show();
                }
                enableRemoveAll(mDeviceList);
                dialog.dismiss();
            };

        }.execute();
    }

    private void getDeviceList() {
        new AsyncTask<Void, String, List<LinkedVMADevices>>() {
            @Override
            protected List<LinkedVMADevices> doInBackground(Void... params) {
            	List<LinkedVMADevices> deviceList = new ArrayList<LinkedVMADevices>();
                deviceList = provMng.syncLinkedDevices();
                if (deviceList.isEmpty()) {
                	return null;
                } else {
                    return deviceList;	
                }
                
                
            };

            protected void onPostExecute(List<LinkedVMADevices> result) {
                mProgresBar.setVisibility(View.GONE);
                if (result != null) {
                	Collections.sort(result, new Comparator<LinkedVMADevices>() {
                    	@Override
            			public int compare(LinkedVMADevices s1, LinkedVMADevices s2) {
            				long date1 = Long.parseLong(s1.createTime);
            				long date2 = Long.parseLong(s2.createTime);
            				if (date1 < date2) {
            					return 1;
            				}
            				return -1;
            			}
                    });
                    mDeviceList = result;
                    mAdapter.notifyDataSetChanged();
                }
                enableRemoveAll(result);
            };

        }.execute();

    }

    /**
     ** This will enlist all device info with name and registered date
     */

    private class DeviceListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public DeviceListAdapter() {
            inflater = LayoutInflater.from(ManageDeviceList.this);
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mDeviceList.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.manage_device_item, parent, false);

                holder = new ViewHolder();
                holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
                holder.createdTime = (TextView) convertView.findViewById(R.id.created_time);
                holder.remove = (Button) convertView.findViewById(R.id.remove);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final LinkedVMADevices deviceInfo = (LinkedVMADevices) getItem(position);
            holder.deviceName.setText(deviceInfo.deviceName);
            String createdTime = MessageUtils.formatTimeStampString(Long.parseLong(deviceInfo.createTime), true);
            holder.createdTime.setText(createdTime);
            holder.remove.setOnClickListener(new RemoveDeviceListener(position));
            return convertView;

        }

        class ViewHolder {
            TextView deviceName;
            Button remove;
            TextView createdTime;

        }

    }

    private class RemoveDeviceListener implements OnClickListener {
        int position;

        public RemoveDeviceListener(int pos) {
            position = pos;
        }

        @Override
        public void onClick(View v) {
            LinkedVMADevices rmvDevice = mDeviceList.get(position);
            deleteLinKedDevice(position, rmvDevice.deviceId, rmvDevice.deviceName);
        }

    }

    public void deleteLinKedDevice(final int pos, final String deviceId, final String deviceName) {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null, getString(R.string.vma_removing_device) + " "
                        + deviceName);
            }

            @Override
            protected Integer doInBackground(Void... params) {

                return provMng.deleteLinkedDevice(deviceId);
            };

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    mDeviceList.remove(pos);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context, R.string.vma_not_remove_conn_problem, Toast.LENGTH_LONG).show();
                }
                enableRemoveAll(mDeviceList);
                dialog.dismiss();
            };

        }.execute();
    }

    protected void enableRemoveAll(List<LinkedVMADevices> deviceList) {
        if (deviceList != null && !deviceList.isEmpty()) {
        	bottomLayout.setVisibility(View.VISIBLE);
            noDeviceFound.setText(R.string.vma_manage_device_desc);
        } else {
        	bottomLayout.setVisibility(View.GONE);
            noDeviceFound.setText(R.string.vma_device_removed_all);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	MessagingPreferenceActivity.setLocale(context);
        super.onConfigurationChanged(newConfig);
    }

}
