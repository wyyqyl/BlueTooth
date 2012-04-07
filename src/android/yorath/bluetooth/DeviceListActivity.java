package android.yorath.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class DeviceListActivity extends Activity {
    private List<String> lstDevices = new ArrayList<String>();
    private ArrayAdapter<String> adtDevices;
    private BluetoothAdapter btAdapter = BlueToothActivity.btAdapter;
    private String TAG = "DeviceListActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.device_list);

	ListView lvBTDevices = (ListView) findViewById(R.id.paired_devices);
	adtDevices = new ArrayAdapter<String>(this,
		android.R.layout.simple_list_item_1, lstDevices);
	lvBTDevices.setAdapter(adtDevices);
	lvBTDevices.setOnItemClickListener(new ItemClickEvent());

	IntentFilter intent = new IntentFilter();
	intent.addAction(BluetoothDevice.ACTION_FOUND);
	intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
	intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	registerReceiver(searchDevices, intent);
    }

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {
	public void onReceive(Context context, Intent intent) {
	    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
		BluetoothDevice device = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		String str = device.getName() + "|" + device.getAddress();
		if (lstDevices.indexOf(str) == -1) { // Prevent duplication
		    lstDevices.add(str);
		    adtDevices.notifyDataSetChanged();
		}
	    }
	}
    };

    class ItemClickEvent implements AdapterView.OnItemClickListener {
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
		long arg3) {
	    btAdapter.cancelDiscovery();
	    String str = lstDevices.get(arg2);
	    String[] values = str.split("\\|");
	    Intent intent = getIntent();
	    intent.putExtra(BlueToothActivity.NAME, values[1]);
	    setResult(RESULT_OK, intent);
	    unregisterReceiver(searchDevices);
	    finish();
	}
    }

    public void onScanButtonClicked(View view) {
	btAdapter.startDiscovery();
	setTitle("Serching for devices...");
    }

    public void onCancelButtonClicked(View view) {
	btAdapter.cancelDiscovery();
	setResult(RESULT_CANCELED);
	finish();
    }

}
