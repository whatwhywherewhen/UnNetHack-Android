package com.tbd.UnNetHack;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

public class Tileset
{
	private Bitmap mBitmap;
	private Bitmap mOverlay;
	private int mTileW;
	private int mTileH;
	private String mTilesetName = "";
	private int mnCols;

	// ____________________________________________________________________________________
	public Tileset()
	{
	}
	
	// ____________________________________________________________________________________
	public void updateTileset(String tilesetName, Resources r)
	{
		if(mTilesetName.equals(tilesetName))
			return;

		boolean TTY = tilesetName.equals("TTY");
		
		if(!TTY)
		{
			int id = r.getIdentifier(tilesetName, "drawable", "com.tbd.UnNetHack");

			if(id > 0)
			{
				mTilesetName = tilesetName;
				
				BitmapDrawable bmpDrawable = (BitmapDrawable)r.getDrawable(id);
				mBitmap = bmpDrawable.getBitmap();
				bmpDrawable = (BitmapDrawable)r.getDrawable(R.drawable.overlays);
				mOverlay = bmpDrawable.getBitmap();
				if(tilesetName.endsWith("32"))
				{
					mTileW = 32;
					mTileH = 32;
				}
				else
				{
					mTileW = 12;
					mTileH = 20;
				}
				
				if(mBitmap == null || mOverlay == null)
					TTY = true;
				else
					mnCols = mBitmap.getWidth() / mTileW;
			}
			else
				TTY = true;
		}
		
		if(TTY)
		{
			mTilesetName = tilesetName;
			mBitmap = null;
			mTileW = 0;
			mTileH = 0;
			mnCols = 0;
		}
	}

	// ____________________________________________________________________________________
	public int getTileBitmapOffset(int iTile)
	{
		if(mBitmap == null)
			return 0;
		
		int iRow = iTile / mnCols;
		int iCol = iTile - iRow * mnCols;

		int x = iCol * mTileW;
		int y = iRow * mTileH;
		
		return (x << 16) | y;
	}
	
	// ____________________________________________________________________________________
	public Bitmap getTile(int iTile)
	{
		if(mBitmap == null)
			return null;
		
		int ofs = getTileBitmapOffset(iTile);
		
		int x = ofs >> 16;
		int y = ofs & 0xffff;		
		
		return Bitmap.createBitmap(mBitmap, x, y, mTileW, mTileH);
	}

	// ____________________________________________________________________________________
	public int getTileWidth()
	{
		return mTileW;
	}
	
	// ____________________________________________________________________________________
	public int getTileHeight()
	{
		return mTileH;
	}
	
	// ____________________________________________________________________________________
	public Bitmap getBitmap()
	{
		return mBitmap;
	}

	// ____________________________________________________________________________________
	public Rect getOverlayRect(short overlay)
	{
		return new Rect(0, 0, 32, 32);
	}

	// ____________________________________________________________________________________
	public Bitmap getTileOverlay(short overlay)
	{
		if(overlay == 8)
			return mOverlay;
		return null;
	}

	// ____________________________________________________________________________________
	public boolean hasTiles()
	{
		return mBitmap != null;
	}
}
