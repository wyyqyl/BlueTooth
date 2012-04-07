package android.yorath.bluetooth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class BlueToothActivity extends Activity {

    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private int requestCode = 1;
    private static BluetoothSocket btSocket;
    public static BluetoothAdapter btAdapter;
    private String TAG = "BlueToothActivity";
    public static final String NAME = "android.yorath.bluetooth.address";
    private int step;
    private readThread t;
    
    private final Double ANGLE_THRESHOLD = 25D;
    private Double angle;
    private Double avgAngle;
    private int count;
    
    private TextView tv;
    private ScrollView sv;
    private AlertDialog dlg;
    private EditText et;
    private Button btnConnect;
    private TextView tvStep;
    private TextView tvAngle;
    
    class readThread extends Thread {

	private volatile boolean isStopped;

	@Override
	public void run() {
	    BufferedReader in = null;
	    String line = null;
	    try {
		in = new BufferedReader(new InputStreamReader(
			btSocket.getInputStream()));
		while (!isStopped && (line = in.readLine()) != null) {
		    Message msg = handler.obtainMessage();
		    msg.obj = line;
		    handler.sendMessage(msg);
		}
	    } catch (IOException e) {
		Log.e(TAG, e.toString());
	    }
	}

	public void stopThread() {
	    isStopped = true;
	}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	tv = (TextView) findViewById(R.id.in);
	sv = (ScrollView) findViewById(R.id.ScrollView01);
	btnConnect = (Button) findViewById(R.id.BtnConnect);
	tvStep = (TextView) findViewById(R.id.tvStep);
	tvAngle = (TextView) findViewById(R.id.tvAngle);
	btAdapter = BluetoothAdapter.getDefaultAdapter();
	if (btAdapter == null) {
	    Toast.makeText(this,
		    "Bluetooth is not supported on this hardware platform",
		    1000).show();
	    onDestroy();
	}
	initAngle();
    }

    @Override
    protected void onDestroy() {

	super.onDestroy();
	// btAdapter.disable();
	android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

	super.onActivityResult(requestCode, resultCode, data);
	if (resultCode == RESULT_OK) {
	    String address = data.getStringExtra(NAME);
	    Log.i(TAG, address);
	    UUID uuid = UUID.fromString(SPP_UUID);
	    BluetoothDevice btDev = btAdapter.getRemoteDevice(address);
	    try {
		btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
		btSocket.connect();
	    } catch (IOException e) {
		Log.e(TAG, e.toString());
	    }
	    btnConnect.setText("Disconnect");
	    Toast.makeText(BlueToothActivity.this, "Connected!", 1000).show();

	    t = new readThread();
	    t.start();
	}
    }

    private final Handler handler = new Handler() {
	private String pre = null;
	private String current = null;

	public void handleMessage(Message msg) {
	    current = (String) msg.obj;
	    if (pre != null) {
		angle = calculateAngle(pre, current);
		if (angle > ANGLE_THRESHOLD) {
		    ++step;
		    tvStep.setText(String.valueOf(step));
		}
	    }
	    DecimalFormat df = new DecimalFormat("#.00");
	    tv.append(current + " " + df.format(angle) +"бу\n");
	    int off = tv.getMeasuredHeight() - sv.getMeasuredHeight();
	    if (off > 0) {
		sv.scrollTo(0, off);
	    }
	    pre = current;
	}
    };

    private OnClickListener onDlgButtonClicked = new OnClickListener() {

	@Override
	public void onClick(DialogInterface dialog, int which) {
	    // http://developer.android.com/guide/topics/ui/dialogs.html#CustomDialog
	    if (et == null) {
		Toast.makeText(BlueToothActivity.this, "EditText is null", 1000)
			.show();
		return;
	    }
	    switch (which) {
	    case Dialog.BUTTON_POSITIVE:
		String fileName = et.getText().toString();
		if (fileName.equals("")) {
		    Toast.makeText(BlueToothActivity.this,
			    "Please fill in the file name", 1000).show();
		    return;
		}

		// Check whether the dir exists
		String dirName = "/sdcard/data/bluetooth/";
		File dir = new File(dirName);
		if (!dir.exists()) {
		    if (!dir.mkdirs()) {
			Toast.makeText(BlueToothActivity.this,
				"Can't create folder", 1000).show();
			return;
		    }
		}

		// Check whether the file exists
		String path = dirName + fileName;
		File file = new File(path);
		if (file.exists()) {
		    Toast.makeText(BlueToothActivity.this, "File exists", 1000)
			    .show();
		} else {
		    try {
			file.createNewFile();
		    } catch (IOException e) {
			Log.e(TAG, e.toString());
		    }
		}

		PrintWriter out = null;
		try {
		    out = new PrintWriter(new FileWriter(path));
		} catch (IOException e) {
		    Log.e(TAG, e.toString());
		}
		out.print(tv.getText());
		out.flush();
		out.close();
		break;
	    case Dialog.BUTTON_NEGATIVE:
	    default:
		break;
	    }
	}
    };

    public void onConnectButtonClicked(View view) {

	if (btAdapter.getState() == BluetoothAdapter.STATE_OFF)
	    btAdapter.enable();
	if (btnConnect.getText().equals("Connect")) {
	    startActivityForResult(new Intent(this, DeviceListActivity.class),
		    requestCode);
	    tv.setText("");
	} else if (btnConnect.getText().equals("Disconnect")) {
	    t.stopThread();
	    t = null;
	    try {
		btSocket.close();
	    } catch (IOException e) {
		Log.e(TAG, e.toString());
	    }
	    btnConnect.setText("Connect");
	}
	onClearButtonClicked(findViewById(R.id.BtnClear));
    }

    public void onSaveButtonClicked(View view) {

	if (btnConnect.getText().equals("Connect")) {
	    Toast.makeText(BlueToothActivity.this,
		    "Please connect to the remote bluetooth first", 1000)
		    .show();
	    return;
	}
	LayoutInflater factory = LayoutInflater.from(this);
	final View dialogview = factory.inflate(R.layout.dialog, null);
	dlg = new AlertDialog.Builder(this).setTitle("Save")
		.setIcon(android.R.drawable.ic_dialog_info).setView(dialogview)
		.setPositiveButton("OK", onDlgButtonClicked)
		.setNegativeButton("Cancel", onDlgButtonClicked).create();
	dlg.show();
	et = (EditText) dlg.findViewById(R.id.filename);
	SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
	String name = df.format(new Date()) + ".txt";
	et.setText(name);
    }

    public void onStepButtonClicked(View view) {
	avgAngle = (avgAngle * count + angle) / (count + 1);
	++count;
	tv.append(TAG + "\n");
	tvAngle.setText(String.valueOf(avgAngle));
    }

    public void onClearButtonClicked(View view) {

	initAngle();
	tv.setText("");
	tvStep.setText("0");
	tvAngle.setText("0.0");
	
    }

    private void initAngle(){
	count = 0;
	angle = 0D;
	avgAngle = 0D;
	step = 0;
    }
    
    public void onQuitButtonClicked(View view) {

	onDestroy();
    }

    private Double calculateAngle(String preData, String currentData) {
	String[] preValues = preData.split(" ");
	String[] currentValues = currentData.split(" ");
	Double vectorProduct = 0d;
	Double preMold = 0d;
	Double currentMold = 0d;
	for (int i = 0; i < 3; ++i) {
	    Double currentPoint = Double.valueOf(currentValues[i]);
	    Double prePoint = Double.valueOf(preValues[i]);
	    vectorProduct += prePoint * currentPoint;
	    currentMold += currentPoint * currentPoint;
	    preMold += prePoint * prePoint;
	}
	currentMold = Math.sqrt(currentMold);
	preMold = Math.sqrt(preMold);

	Double cosAngle = vectorProduct / (currentMold * preMold);
	Double angle = Math.toDegrees(Math.acos(cosAngle));
	return angle;
    }
}