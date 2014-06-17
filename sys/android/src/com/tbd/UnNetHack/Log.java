package com.tbd.UnNetHack;

public class Log
{
	// ____________________________________________________________________________________
	public static void print(String string)
	{
		if(DEBUG.isOn())
			android.util.Log.i("UnNetHack", string);
	}

	// ____________________________________________________________________________________
	public static void print(int i)
	{
		if(DEBUG.isOn())
			print(Integer.toString(i));
	}
}
