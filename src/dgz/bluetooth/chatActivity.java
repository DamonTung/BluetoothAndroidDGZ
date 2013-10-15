package dgz.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.UUID;
import java.io.*;

import dgz.bluetooth.R;
import dgz.bluetooth.Bluetooth.ServerOrCilent;
import android.R.string;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class chatActivity extends Activity implements OnItemClickListener,
		OnClickListener {
	/** Called when the activity is first created. */

	private ListView mListView;
	private ArrayList<deviceListItem> list;
	private Button sendButton;
	private Button disconnectButton;
	private EditText editMsgView;
	deviceListAdapter mAdapter;
	Context mContext;

	/* һЩ��������������������� */
	public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
	public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
	public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
	public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

	private BluetoothServerSocket mserverSocket = null;
	private ServerThread startServerThread = null;
	private clientThread clientConnectThread = null;
	public static BluetoothSocket socket = null;
	private BluetoothDevice device = null;
	private readThread mreadThread = null;;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		mContext = this;
		init();
	}

	private void init() {
		list = new ArrayList<deviceListItem>();
		mAdapter = new deviceListAdapter(this, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		mListView.setFastScrollEnabled(true);
		editMsgView = (EditText) findViewById(R.id.MessageText);
		editMsgView.clearFocus();

		sendButton = (Button) findViewById(R.id.btn_msg_send);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String msgText = editMsgView.getText().toString();
				if (msgText.length() > 0) {
					sendMessageHandle(msgText);
					editMsgView.setText("");
					editMsgView.clearFocus();
					// close InputMethodManager
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editMsgView.getWindowToken(), 0);
				} else
					Toast.makeText(mContext, "�������ݲ���Ϊ�գ�", Toast.LENGTH_SHORT)
							.show();
			}
		});

		disconnectButton = (Button) findViewById(R.id.btn_disconnect);
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
					shutdownClient();
				} else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {
					shutdownServer();
				}
				Bluetooth.isOpen = false;
				Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
				Toast.makeText(mContext, "�ѶϿ����ӣ�", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private Handler LinkDetectedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Toast.makeText(mContext, (String)msg.obj,
			// Toast.LENGTH_SHORT).show();
			if (msg.what == 1) {
				list.add(new deviceListItem((String) msg.obj, true));
			} else {
				list.add(new deviceListItem((String) msg.obj, false));
			}
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
		}

	};

	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (Bluetooth.isOpen) {
			Toast.makeText(mContext, "�����Ѿ��򿪣�����ͨ�š����Ҫ�ٽ������ӣ����ȶϿ���",
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
			String address = Bluetooth.BlueToothAddress;
			if (!address.equals("null")) {
				device = mBluetoothAdapter.getRemoteDevice(address);
				clientConnectThread = new clientThread();
				clientConnectThread.start();
				Bluetooth.isOpen = true;
			} else {
				Toast.makeText(mContext, "address is null !",
						Toast.LENGTH_SHORT).show();
			}
		} else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {
			startServerThread = new ServerThread();
			startServerThread.start();
			Bluetooth.isOpen = true;
		}
	}

	// �����ͻ���
	private class clientThread extends Thread {
		public void run() {
			try {
				// ����һ��Socket���ӣ�ֻ��Ҫ��������ע��ʱ��UUID��
				// socket =
				// device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
				socket = device.createRfcommSocketToServiceRecord(UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				// ����
				Message msg2 = new Message();
				msg2.obj = "���Ժ��������ӷ�����:" + Bluetooth.BlueToothAddress;
				msg2.what = 0;
				LinkDetectedHandler.sendMessage(msg2);

				socket.connect();

				Message msg = new Message();
				msg.obj = "�Ѿ������Ϸ���ˣ����Է�����Ϣ��";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
				
				// ������������
				mreadThread = new readThread();
				mreadThread.start();
				
				Message msgHello=new Message();
				msgHello.obj="���е�Ƭ��������������#h";//���е�Ƭ��
				msgHello.what=0;
				LinkDetectedHandler.sendMessage(msgHello);
				
				sendMessageHandle("#h");
				
				
				Message msgHelloWait=new Message();
				msgHelloWait.obj="�ȴ���Ƭ����Ӧ�����������";
				msgHelloWait.what=0;
				LinkDetectedHandler.sendMessage(msgHelloWait);
				
				String strHelloString=null;
				while(strHelloString==null){
					strHelloString=mreadThread.getString("#o");
				}
				
				
				Message msgConnectionMessage=new Message();
				msgConnectionMessage.obj="���е�Ƭ���ɹ������ͽ�����������#c��";//�뵥Ƭ����������
				msgConnectionMessage.what=0;
				LinkDetectedHandler.sendMessage(msgConnectionMessage);
				sendMessageHandle("#c");
				
				Message msgConnection=new Message();
				msgConnection.obj="�ȴ���Ƭ����Ӧ���������������";
				msgConnection.what=0;
				LinkDetectedHandler.sendMessage(msgConnection);
				
				String strConnectionString=null;
				while(strConnectionString==null){
					
					strConnectionString=mreadThread.getString("#k");
					
				}
				
				Message msgRequestDataMessage=new Message();
				msgRequestDataMessage.obj="�������ӳɹ���׼���������ݣ���������#r";
				msgRequestDataMessage.what=0;
				sendMessageHandle(msgRequestDataMessage.obj.toString());
				sendMessageHandle("#r");
				
				//String strDataString=mreadThread.getString("@e");
				Intent intentChtoViewIntent =new Intent(chatActivity.this,dataViewActivity.class);
				startActivity(intentChtoViewIntent);
				
			} catch (IOException e) {
				Log.e("connect", "", e);
				Message msg = new Message();
				msg.obj = "���ӷ�����쳣���Ͽ�����������һ�ԡ�";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
			}
		}
	};

	// ����������
	private class ServerThread extends Thread {
		public void run() {

			try {
				/*
				 * ����һ������������ �����ֱ𣺷��������ơ�UUID
				 */
				mserverSocket = mBluetoothAdapter
						.listenUsingRfcommWithServiceRecord(
								PROTOCOL_SCHEME_RFCOMM,
								UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

				Log.d("server", "wait cilent connect...");

				Message msg = new Message();
				msg.obj = "���Ժ����ڵȴ��ͻ��˵�����...";
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);

				/* ���ܿͻ��˵��������� */
				socket = mserverSocket.accept();
				Log.d("server", "accept success !");

				Message msg2 = new Message();
				String info = "�ͻ����Ѿ������ϣ����Է�����Ϣ��";
				msg2.obj = info;
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg2);
				
				/*Toast.makeText(mContext, "���е�Ƭ���������ȴ���Ӧ����", Toast.LENGTH_SHORT).show();
				
				Message msg3=new Message();
				msg3.obj="#h";//���е�Ƭ��
				msg3.what=0;
				LinkDetectedHandler.sendMessage(msg3);*/
				
				// ������������
				mreadThread = new readThread();
				mreadThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	/* ֹͣ������ */
	private void shutdownServer() {
		new Thread() {
			public void run() {
				if (startServerThread != null) {
					startServerThread.interrupt();
					startServerThread = null;
				}
				if (mreadThread != null) {
					mreadThread.interrupt();
					mreadThread = null;
				}
				try {
					if (socket != null) {
						socket.close();
						socket = null;
					}
					if (mserverSocket != null) {
						mserverSocket.close();/* �رշ����� */
						mserverSocket = null;
					}
				} catch (IOException e) {
					Log.e("server", "mserverSocket.close()", e);
				}
			};
		}.start();
	}

	/* ֹͣ�ͻ������� */
	private void shutdownClient() {
		new Thread() {
			public void run() {
				if (clientConnectThread != null) {
					clientConnectThread.interrupt();
					clientConnectThread = null;
				}
				if (mreadThread != null) {
					mreadThread.interrupt();
					mreadThread = null;
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
			};
		}.start();
	}

	// ��������
	private void sendMessageHandle(String msg) {
		if (socket == null) {
			Toast.makeText(mContext, "û������", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			OutputStream os = socket.getOutputStream();
			os.write(msg.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		list.add(new deviceListItem(msg, false));
		mAdapter.notifyDataSetChanged();
		mListView.setSelection(list.size() - 1);
	}

	// ��ȡ����
	private class readThread extends Thread {
		Hashtable<String,String> hMap=new Hashtable<String, String>();
		Iterator<String> iteratorHMap = hMap.keySet().iterator();
		
		public readThread() {
			// TODO �Զ����ɵĹ��캯�����
		}
		
		//@SuppressWarnings("unused")
		public String getString(String str){
			
			return hMap.get(str);
		}
		
		public String getDataString(){
			while(iteratorHMap.hasNext()){
				return iteratorHMap.next();
			}
			return null;
		}
		
		public void delElement(String str) {
			if(str==iteratorHMap.next())
				iteratorHMap.remove();			
		}

		public void run() {

			byte[] buffer = new byte[1024];
			int bytes;
			InputStream mmInStream = null;

			try {
				mmInStream = socket.getInputStream();
				InputStreamReader input = new InputStreamReader(mmInStream);
				BufferedReader reader = new BufferedReader(input);
				
				String s;
				while ((s = reader.readLine()) !=null) {
					
//					Thread.sleep(1000);
					
					if(s.indexOf("@s")==-1){
						hMap.put(s, s);
						
						Message msg=new Message();
						msg.obj=s;
						msg.what=1;
						LinkDetectedHandler.sendMessage(msg);
					}
					else {
						Message msg=new Message();
						msg.obj=s;
						msg.what=1;
						LinkDetectedHandler.sendMessage(msg);
						
//						Message msgV=new Message();
//						msgV.obj=s;
//						msgV.what=2;
						dataViewActivity.Two.obtainMessage(2, s).sendToTarget();
						
					}
					
					/*Message msg = new Message();
					msg.obj = s;
					msg.what = 1;
					while(!(msg.obj.toString()).equalsIgnoreCase("#o")){
						Message msgMessage=new Message();
						msgMessage.obj="�����ȴ���Ƭ����Ӧ�����������";
						msgMessage.what=1;
						LinkDetectedHandler.sendMessage(msgMessage);
						//break;
					}
					String infoString="#c";
					msg.obj=infoString;
					LinkDetectedHandler.sendMessage(msg);
					while(!(msg.obj.toString()).equalsIgnoreCase("#k")){
						Message msgMessage=new Message();
						msgMessage.obj="�����ȴ���Ƭ����Ӧ���������������";
						msgMessage.what=1;
						LinkDetectedHandler.sendMessage(msgMessage);
					}
					infoString="#r";
					msg.obj=infoString;
					LinkDetectedHandler.sendMessage(msg);
					Message msgMessage=new Message();
					msgMessage.obj="�����ȴ���Ƭ���������ݡ�����";
					msgMessage.what=1;
					LinkDetectedHandler.sendMessage(msgMessage);
					
					//if(!s.equalsIgnoreCase("#o"))break;
					Message msg2 = new Message();
					msg2.obj = s;
					msg2.what = 2;*/
					//dataViewActivity.MyHandler.handleMessage2(msg2);
					/*final String str=s;
					new Thread() {
						public void run() {
							try {
								Message msg2 = new Message();
								msg2.obj = str;
								msg2.what = 2;
								dataViewActivity.MyHandler.handleMessage2(msg2);
							} catch (Exception e) {
								// TODO: handle exception
							}
						}
					}.start();*/
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			/*
			 * while (true) { try { // Read from the InputStream if ((bytes =
			 * mmInStream.read(buffer)) > 0) { byte[] buf_data = new
			 * byte[bytes]; for (int i = 0; i < bytes; i++) { buf_data[i] =
			 * buffer[i]; } //final String s = new String(buf_data); Message msg
			 * = new Message(); msg.obj = s; msg.what = 1;
			 * LinkDetectedHandler.sendMessage(msg); new Thread(){ public void
			 * run() { try { Message msg2 = new Message(); msg2.obj = s;
			 * msg2.what = 2; dataViewActivity.MyHandler.handleMessage2(msg2); }
			 * catch (Exception e) { // TODO: handle exception } } }.start(); //
			 * add start
			 * 
			 * // end } } catch (IOException e) { try { Log.v("dgz",
			 * "����read�̳߳�����"); mmInStream.close(); } catch (IOException e1) {
			 * // TODO Auto-generated catch block Log.v("dgz", "����read�̳߳�����");
			 * e1.printStackTrace();
			 * 
			 * } break; } }
			 */ catch (Exception e) {
				// TODO �Զ����ɵ� catch ��
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
			shutdownClient();
		} else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {
			shutdownServer();
		}
		Bluetooth.isOpen = false;
		Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
	}

	public class SiriListItem {
		String message;
		boolean isSiri;

		public SiriListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}

	public class deviceListItem {
		String message;
		boolean isSiri;

		public deviceListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}
}