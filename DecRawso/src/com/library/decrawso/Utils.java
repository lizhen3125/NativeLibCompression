package com.library.decrawso;

import android.content.res.AssetManager;
/**
 *π§æﬂ¿‡
 *yangjing
 */	
public class Utils {

	private native int Decode(AssetManager  asset,String inpath, String outpath,String abi); //will unzip (*.7z) in the same folder with full path
	private native boolean IsArmMode(); //get unzip arm or x86 folder
	private native void SetFilter(String filter,String fix);
	
	public int decode(AssetManager  asset,String inpath, String outpath,String abi) {
		return Decode(asset, inpath, outpath, abi);
	}
	
	public boolean isArmMode() {
		return IsArmMode();
	}
	
	public void setFilter(String filter,String fix) {
		SetFilter(filter, fix);
	}
}
