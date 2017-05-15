package com.library.decrawso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

/**
 * yangjing
 * 按照需求，抽象抽来的新的API 接口调用方式
 */

public class DecSoHelper {
//	private native int DecodeNew(AssetManager  asset,String inpath, String outpath,String abi); //will unzip (*.7z) in the same folder with full path
//	private native boolean IsArmModeNew(); //get unzip arm or x86 folder
//	private native void SetFilterNew(String filter,String fix);
	
	private static DecSoHelper DecSoHelperSingleton = null;
	
	private String sPathName=null;
	private String sAppFilePath=null;
	
	private Thread mDec7zLibThread=null;
	private int localVersion=0;
	private long lasttime=0;
	private String abi=null;
	private Context mAppContext;
	private Handler mHdl;
	private ProgressDialog dProDlg;
	
	private UtilsFunc mUtils = new UtilsFunc();
	private CloudDownloader mCloudDlr = new CloudDownloader();
	
	private final static int HDL_MSGBASE = 54321;	
	//解压结束后的消息
	public final static int  HDL_MSGDECEND = 1+HDL_MSGBASE; 
	//hack lib path 结束后的消息
	public final static int HDL_MSG_HACK_END = 2 + HDL_MSGBASE;
	
	//解压过程中，可能出现的错误类型
	//首期，会对所有的错误类型，加以统计，以确定后面该如何进行修改调整
	public final static int  SZ_OK = 0;
	public final static int  SZ_ERROR_DATA = 1;
	public final static int  SZ_ERROR_MEM = 2;
	public final static int  SZ_ERROR_CRC = 3;
	public final static int  SZ_ERROR_UNSUPPORTED = 4;
	public final static int  SZ_ERROR_PARAM = 5;
	public final static int  SZ_ERROR_INPUT_EOF = 6;
	public final static int  SZ_ERROR_OUTPUT_EOF = 7;
	public final static int  SZ_ERROR_READ = 8;
	public final static int  SZ_ERROR_WRITE = 9;
	public final static int  SZ_ERROR_PROGRESS = 10;
	public final static int  SZ_ERROR_FAIL = 11;
	public final static int  SZ_ERROR_THREAD = 12;
	public final static int  SZ_ERROR_ARCHIVE = 16;
	public final static int  SZ_ERROR_NO_ARCHIVE = 17;	
	public final static int  SZ_FILE_NOT_OPENED = 20;
	public final static int  SZ_FLAG_OK_END_ERROR = 21;//写解压成功标志文件出错
	public final static int  SZ_FLAG_ARM_END_ERROR = 22;//写arm文件标志出错
	public final static int SZ_UNSATISFIED_LINK_ERROR = 23;//UnsatisfiedLinkError

	
	private DecSoHelper(Context context, Handler hdl) {
		localVersion = 0;
		dProDlg = null;
		mDec7zLibThread = null;
		
		mHdl = hdl;
		mAppContext = context.getApplicationContext();

		sAppFilePath = context.getFilesDir().getAbsolutePath();
		sPathName  = sAppFilePath+"/DecRawsoLib/";
		AssetFileDescriptor fd=null;
 
		abi = android.os.Build.CPU_ABI;
		if(!abi.contains("arm") && !abi.contains("x86") && !abi.contains("mips") && !abi.contains("x32"))
			abi="armeabi";	
		//todo:  x86 now will detect as armeabi-v7a
		String tmpx86abi = getX86abi();
		if (tmpx86abi!=null) {
			abi = tmpx86abi;
			if(abi.contains("x32"))
				abi = "x86";
		}
		
		//may error , so fd = null
		AssetManager am = mAppContext.getAssets();
		try {
			fd = am.openFd("rawso");
			fd.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	
		if (fd!=null) {
	        try {  
	            PackageInfo packageInfo = context.getApplicationContext()  
	                    .getPackageManager().getPackageInfo(context.getPackageName(), 0);  
	            localVersion = packageInfo.versionCode;  
	            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.GINGERBREAD)
	            	lasttime = getLastTime(packageInfo);
	        } catch (NameNotFoundException e) {  
	            e.printStackTrace(); 
	            localVersion = 0;
	            lasttime = 0;
	        } 				
	        File filex = new File(sAppFilePath+"/DecRawsoLib/decdone_"+localVersion+"_"+lasttime);
	        File filedir = new File(sAppFilePath+"/DecRawsoLib/");
    		if (!filex.exists()) {
         		if (!filedir.exists()) {  
        			filedir.mkdir();//empty so create dir
         		} else { //delete all sub files
        			File forcearm = new File(sAppFilePath+"/DecRawsoLib/_FORCEARM_.tmp");
        			if(forcearm.exists() && abi.contains("x86")) { //x86 lib miss, we can use arm lib
        				abi="armeabi-v7a";
        			}
        				
	    			File[] allfiles = filedir.listFiles();
	    			for (File tmpfile : allfiles) {
	    				tmpfile.delete();  //_FORCEARM_.tmp will be deleted
	    			}
        		}
    		}
		} else {//被解压缩的文件，打开失败
			sendDecEndMsg(SZ_FILE_NOT_OPENED);
		}
	}
	public static DecSoHelper getInstance(Context ctx, Handler handler) {
		if (DecSoHelperSingleton ==null ) {
			DecSoHelperSingleton = new DecSoHelper(ctx, handler);	
		} else {//更新handler
			DecSoHelperSingleton.updateHandler(handler);
		}
		return DecSoHelperSingleton;
	}
	
	private void updateHandler(Handler handler) {
		mHdl = handler;
	}
	
	public void cancelHandler() {
		mHdl = null;
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private long getLastTime(PackageInfo packageInfo){
		return packageInfo.lastUpdateTime;
	}	
	
	private String getX86abi() {   //someone say : build.prop abi can be changed
		/*String x86abi = android.os.Build.CPU_ABI;
	
		if(x86abi.contains("x86")||x86abi.contains("x32"))
			return x86abi;
		else //if(x86abi.contains("armeabi-v7a")) //avoid any changes*/   //-- now, 64bit will return 32bit abi
		{
			Process process;
			try {
				process = Runtime.getRuntime().exec("getprop ro.product.cpu.abi");
	            InputStreamReader ir = new InputStreamReader(process.getInputStream());
	            BufferedReader input = new BufferedReader(ir);
	            String tmpabi = input.readLine();
	            input.close();
	            
	            if (tmpabi.contains("x86") || tmpabi.contains("x32"))
	            	return tmpabi;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return null;
	}	

	//进行解压缩操作
	public void doDec7zLib() {
		File filex = new File(sAppFilePath+"/DecRawsoLib/decdone_"+localVersion+"_"+lasttime);
		if (filex.exists()) {//done文件已经存在，表示已经解压缩成功
			sendDecEndMsg(SZ_OK);
			return;
		}
		
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD)
			System.loadLibrary("DecRawso22");
		else
			System.loadLibrary("DecRawso");		

		mDec7zLibThread = new Thread(new Dec7zLibThread());
		mDec7zLibThread.start();
	}
	
	//进行解压缩的runnable
	class Dec7zLibThread implements Runnable	{
		private int readRawso(String outname) {
			AssetManager am = mAppContext.getAssets();
			try {
				BufferedInputStream bin = new BufferedInputStream(am.open("rawso"));
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(new File(outname))); 
				if (bin!=null && bout!=null) {
		            // cache 
		            byte[] b = new byte[1024 * 4];
		            int len;
		            while ((len = bin.read(b)) != -1) {
		            	bout.write(b, 0, len);
		            }
		            // refresh 
		            bout.flush(); 
		            bin.close();
		            bout.close();
				} else {
					return SZ_ERROR_WRITE;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return SZ_ERROR_WRITE;
			}
			return SZ_OK;
		}
		
		@Override
		public void run() { 
			int res;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) { //decode on android2.2
				res = readRawso(sAppFilePath+"/DecRawsoLib/rawso22");
				if (res==SZ_OK)
					res = new Utils().decode(null,sAppFilePath+"/DecRawsoLib/rawso22",sPathName,abi);
			} else {
				res = new Utils().decode(mAppContext.getAssets(),null,sPathName,abi);
			}
			
        	if (SZ_OK == res) {
        		File filex = new File(sAppFilePath+"/DecRawsoLib/decdone_"+localVersion+"_"+lasttime);
        		try {
					filex.createNewFile();
					filex = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					res = SZ_FLAG_OK_END_ERROR;
				}
        	}
			
			if (new Utils().isArmMode()) {
				File file_armmode = new File(sAppFilePath+"/DecRawsoLib/armmode");
				try {
					file_armmode.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					res = SZ_FLAG_ARM_END_ERROR;
				}
			}
			
    		if (Build.VERSION.SDK_INT<Build.VERSION_CODES.GINGERBREAD) {//decode on android2.2
    			File fileraw22 = new File(sAppFilePath+"/DecRawsoLib/rawso22");
    			if (fileraw22.exists())
    				fileraw22.delete();
    			fileraw22 = null;
    		}
    		
    //		mUtils.HackLibPath(sPathName); //only decoding finish then add library path, to avoid load a decoding file 
        	
        	if (mHdl != null) {
        		sendDecEndMsg(res);
        	}
		}
	}
	
	//解压结束之后，发送的消息
	//res为0时表示成功，其他为非成功
	private void sendDecEndMsg( int arg1) {
		if (mHdl!=null)
        	mHdl.sendMessage(mHdl.obtainMessage(HDL_MSGDECEND, arg1, 0));	
	}
	
	//只有解压缩成功之后，才能调用
	public void hackLibPath( ) {
		String hackResult = mUtils.HackLibPath(sPathName);
		Message msg = mHdl.obtainMessage(HDL_MSG_HACK_END);
		msg.obj = hackResult;
		mHdl.sendMessage(msg);
	}
	
}
	