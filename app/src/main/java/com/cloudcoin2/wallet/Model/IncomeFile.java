package com.cloudcoin2.wallet.Model;

import android.net.Uri;

public class IncomeFile {

	public static int TYPE_PNG = 1;
	public static int TYPE_STACK = 2;

	public IncomeFile(String fileName, int fileType) {
		this.fileName = fileName;
		this.fileType = fileType;
	}
	public String fileName;
	public int fileType;


	
}
