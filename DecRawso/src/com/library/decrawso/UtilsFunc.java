package com.library.decrawso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

public class UtilsFunc {
	private Context mUtilsFuncContext;

	class ToastThread implements Runnable	
	{
		private String msgstr;
		private Looper lp;
		public ToastThread(String _msgstr,Context mc)
		{
			msgstr = _msgstr;
			mUtilsFuncContext = mc;
		}
		public Looper getItLooper()
		{
			return lp;
		}
		@Override
		public void run() {   
			Looper.prepare();  
			lp = Looper.myLooper();
	        Toast.makeText(mUtilsFuncContext,msgstr,Toast.LENGTH_LONG).show();  
	        Looper.loop(); 
		}
	}
	public boolean showToastInThread(String msgstr,Context mc)
	{
		ToastThread runToast = new ToastThread(msgstr,mc);
		Thread tmpToastThread = new Thread(runToast);
		tmpToastThread.start();
		try {
			tmpToastThread.join(5000);
			runToast.getItLooper().quit();
			tmpToastThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;
	}
	
	public String HackLibPath(String pname) {
		/*
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        	HackSystemICS(pname);	
        else
        {
        	if(!HackSystemLow1(pname))
        	{
        		if(!HackSystemLow2(pname))
        			HackSystemLow3(pname);
        	}
        } 		
		*/ //--leyou find sansumg s4 will use HackSystemLow1?
		String hackResult = HackSystemICS(pname);
		if (!TextUtils.isEmpty(hackResult)) {//ics hack failed
			hackResult = HackSystemLow1(pname);
			if (!TextUtils.isEmpty(hackResult)) {// low1 hack failed
				hackResult = HackSystemLow2(pname);
				if (!TextUtils.isEmpty(hackResult)) {//low2 hack failed
					hackResult = HackSystemLow3(pname);
				}
			}
		}
		return hackResult;
	}
	
	@SuppressLint("NewApi")
	private String HackSystemICS(String pname) {
		String result = null;
		try {
			Field fieldSysPath = BaseDexClassLoader.class.getDeclaredField("pathList");  
	        fieldSysPath.setAccessible(true);
	        Object paths = (Object)fieldSysPath.get(this.getClass().getClassLoader());  
	        Class c = paths.getClass();
	        Field Libpaths = c.getDeclaredField("nativeLibraryDirectories");
	        Libpaths.setAccessible(true);
	        
	        File[] nativepaths = (File[])Libpaths.get(paths);
	        //if exists, no add
	        for(File path : nativepaths) {
	        	if (path.getPath().equals(new File(pname).getPath())) {
	        		return result;
	        	}
	        }
	        
	        File[] tmp = new File[nativepaths.length+1];     
	        System.arraycopy(nativepaths,0,tmp,1,nativepaths.length);     
	        tmp[0] = new File(pname);    
	        Libpaths.set(paths, tmp);

		} catch (Exception e) {
			result = "SystemICS: " + e.getMessage();
			e.printStackTrace();
		} catch (java.lang.Error e) {//NoClassDefFoundError
			result = "SystemICS: " + e.getMessage();
			e.printStackTrace();
		}
		return result;
	}
	
	private String HackSystemLow3(String pname) {//even older
		Field fieldSysPath;
		String result = null;
		
		try {
			fieldSysPath = DexClassLoader.class.getDeclaredField("mLibPaths");  
	        fieldSysPath.setAccessible(true);
	        
	        String[] paths = (String[])fieldSysPath.get(this.getClass().getClassLoader());  
	        
	        //if exists, no add
	        for(String path : paths) {
	        	if (path.equals(pname)) {
	        		return result;
	        	}
	        }
	        
	        String[] tmp= new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);     
	        tmp[paths.length] = pname;
	        fieldSysPath.set(this.getClass().getClassLoader(), tmp);

		} catch (Exception e) {
			result = "SystemLow3: " + e.getMessage() ;
			e.printStackTrace();
		} catch (java.lang.Error e) {//NoClassDefFoundError
			result = "SystemLow3: " + e.getMessage() ;
			e.printStackTrace();
		}
		return result;
	}	
	
	private String HackSystemLow2(String pname) { //for 2.2
		String result = null;
		try {
			Field fieldSysPath = PathClassLoader.class.getDeclaredField("mLibPaths");  
	        fieldSysPath.setAccessible(true);
	        
	        String[] paths = (String[])fieldSysPath.get(this.getClass().getClassLoader());  
	        //if exists, no add
	        for(String path : paths) {
	        	if (path.equals(pname)) {
	        		return result;
	        	}
	        }
	        
	        String[] tmp= new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);     
	        tmp[paths.length] = pname;
	        fieldSysPath.set(this.getClass().getClassLoader(), tmp);

		} catch (Exception e) {
			result = "SystemLow2: " + e.getMessage();
			e.printStackTrace();
		} catch (java.lang.Error e) {//NoClassDefFoundError
			result ="SystemLow2: " +  e.getMessage();
			e.printStackTrace();
		}
		return result;
	}	
	
	private String HackSystemLow1(String pname)  {//for 2.3
		String result = null;
		try{
			Field fieldSysPath = PathClassLoader.class.getDeclaredField("libraryPathElements");  
	        fieldSysPath.setAccessible(true);
	        
	        List<String> paths = (List<String>)fieldSysPath.get(this.getClass().getClassLoader());  
	        //if exists, no add
	        if (paths.contains(pname)) {
	        	return result;
	        }
	        paths.add(pname);
	        //fieldSysPath.set(paths, paths);

		} catch (Exception e) {
			result = "SystemLow1: " + e.getMessage();
			e.printStackTrace();
		} catch (java.lang.Error e)  {//NoClassDefFoundError
			result = "SystemLow1: " + e.getMessage();
			e.printStackTrace();
		}
		return result;
	}	

	public int getIdByName(Context context, String className, String name) {
		String packageName = context.getPackageName();
		Class r = null;
		int id = 0;
		try {
			r = Class.forName(packageName + ".R");

			Class[] classes = r.getClasses();
			Class desireClass = null;

			for (int i = 0; i < classes.length; ++i) {
				if (classes[i].getName().split("\\$")[1].equals(className)) {
					desireClass = classes[i];
					break;
				}
			}

			if (desireClass != null)
				id = desireClass.getField(name).getInt(desireClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}

		return id;
	}
}
