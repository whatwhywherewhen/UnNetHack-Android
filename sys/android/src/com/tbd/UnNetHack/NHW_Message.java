package com.tbd.UnNetHack;

import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NHW_Message implements NH_Window {
	protected static final int SHOW_MAX_LINES = 3;

	private class LogEntry {
		public String msg;
		public int repeat;
	}

	private NetHackIO mIO;
	private Activity mContext;
	private final int MaxLog = 256;
	private LogEntry[] mLog = new LogEntry[MaxLog];
	private int mCurrentIdx;
	private int mLogCount;
	private int mDispCount;
	private UI mUI;
	private NHW_Text mLogView;
	private boolean mIsVisible;
	private int mWid;

	// ____________________________________________________________________________________
	public NHW_Message( Activity context, NetHackIO io ) {
		mIO = io;
		for( int i = 0; i < mLog.length; i++ )
			mLog[i] = new LogEntry();
		setContext(context);
	}

	// ____________________________________________________________________________________
	public String getTitle() {
		return "NHW_Message";
	}

	// ____________________________________________________________________________________
	public void setContext( Activity context ) {
		if( mContext == context )
			return;
		mContext = context;
		mUI = new UI();
		if( mIsVisible )
			mUI.showInternal();
		else
			mUI.hideInternal();
		if( mLogView != null )
			mLogView.setContext(context);
	}

	// ____________________________________________________________________________________
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput) {
		KeyEventResult ret;
		if( isLogShowing() && (ret = mLogView.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput)) != KeyEventResult.IGNORED )
			return ret;
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, bSoftInput) ? KeyEventResult.HANDLED : KeyEventResult.IGNORED;
	}

	// ____________________________________________________________________________________
	public void clear() {
		mDispCount = 0;
		mUI.clear();
	}

	// ____________________________________________________________________________________
	private int getIndex( int i ) {
		if( mLogCount == 0 )
			return 0;
		return i & (MaxLog - 1);
	}

	// ____________________________________________________________________________________
	@Override
	public void printString( int attr, String str, int append, int color ) {
		mCurrentIdx = getIndex(mLogCount - 1);

		if( append < 0 && mLogCount > 0 ) {
			append++;
			String l = mLog[mCurrentIdx].msg;
			if( append < -l.length() )
				append = -l.length();
			l = l.substring(0, l.length() + append);
			mLog[mCurrentIdx].msg = l + str;
		} else if( append > 0 && mLogCount > 0 ) {
			if( str.length() > 0 )
				mLog[mCurrentIdx].msg = mLog[mCurrentIdx].msg + str;
		} else {
			String newMsg = str;
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			boolean grouping = prefs.getBoolean("groupLog", false);
			if( grouping && mLogCount > 1 ) {
				String prev = mLog[mCurrentIdx].msg;
				if( prev.toString().equals(newMsg.toString()) ) {
					mLog[mCurrentIdx].repeat++;
					mDispCount++;
				} else
					addMessage(newMsg);
			} else
				addMessage(newMsg);
		}
		mUI.update();
	}

	// ____________________________________________________________________________________
	@Override
	public void setCursorPos( int x, int y ) {
	}

	// ____________________________________________________________________________________
	private void addMessage( String newMsg ) {
		mCurrentIdx = getIndex(mCurrentIdx + 1);
		mLog[mCurrentIdx].msg = newMsg;
		mLog[mCurrentIdx].repeat = 1;
		mDispCount++;
		mLogCount++;
	}

	// ____________________________________________________________________________________
	public String getLogLine( int maxLineCount ) {
		if( mDispCount <= 0 )
			return "";

		int nLines = Math.min(mDispCount, maxLineCount);

		StringBuilder line = new StringBuilder();
		for( int i = nLines - 1; i >= 0; i-- ) {
			int idx = getIndex(mCurrentIdx - i);
			line.append(mLog[idx].msg);
			line.append(' ');
		}
		line.append('\n');
		return line.toString();
	}

	// ____________________________________________________________________________________
	public void show( boolean bBlocking ) {
		mIsVisible = true;
		mUI.showInternal();
		if( bBlocking ) {
			// unblock immediately
			mIO.sendKeyCmd(' ');
		}
	}

	// ____________________________________________________________________________________
	public void destroy() {
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	public void showLog(boolean bBlocking) {

		if( mLogView == null )
			mLogView = new NHW_Text(0, mContext, mIO);

		boolean highlightNew = mDispCount > SHOW_MAX_LINES;

		int nLogs = 0;
		for( int n = 0; n < MaxLog; n++ ) {
			if( mLog[n].msg != null )
				nLogs++;
		}

		int attr = 0;
		mLogView.clear();
		int i = mCurrentIdx + 1;
		for( int n = 0; n < MaxLog; n++, i++ ) {
			LogEntry e = mLog[getIndex(i)];
			String s = e.msg;
			if( s != null ) {
				nLogs--;
				if( highlightNew && nLogs < mDispCount )
					attr = TextAttr.ATTR_BOLD;
				if( e.repeat > 1 )
					mLogView.printString(attr, s + " (" + Integer.toString(e.repeat) + ")", 0, 0xffffffff);
				else
					mLogView.printString(attr, s, 0, 0xffffffff);
			}
		}
		mLogView.show(bBlocking);
		mLogView.scrollToEnd();
		int disp = Math.min(SHOW_MAX_LINES, mDispCount);
		clear();
		mDispCount = disp;
		mUI.update();
	}

	// ____________________________________________________________________________________
	private boolean isLogShowing() {
		return mLogView != null && mLogView.isVisible();
	}

	// ____________________________________________________________________________________
	public void setId( int wid ) {
		mWid = wid;
	}

	// ____________________________________________________________________________________
	public int id() {
		return mWid;
	}

	// ____________________________________________________________________________________
	// //
	// //
	// ____________________________________________________________________________________
	// //
	private class UI {
		private TextView m_view;
		private TextView m_more;
		private boolean mTextUpdaterRunning;

		// ____________________________________________________________________________________
		public UI() {
			m_view = (TextView)mContext.findViewById(R.id.nh_message);
			m_more = (TextView)mContext.findViewById(R.id.more);
			m_more.setVisibility(View.GONE);
			m_more.setOnClickListener(new OnClickListener() {
				public void onClick( View v ) {
					showLog(false);
				}
			});
		}

		// ____________________________________________________________________________________
		public boolean isMoreVisible() {
			return m_more.getVisibility() == View.VISIBLE;
		}

		// ____________________________________________________________________________________
		public void showInternal() {
			update();
			m_view.setVisibility(View.VISIBLE);
		}

		// ____________________________________________________________________________________
		public void hideInternal() {
			// m_view.setVisibility(View.INVISIBLE);
			// m_more.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void clear() {
			m_more.setVisibility(View.GONE);
			m_view.setText("");
		}

		// ____________________________________________________________________________________
		public void update() {
			if( !mTextUpdaterRunning )
				mTextUpdater.run();
			if( mDispCount > SHOW_MAX_LINES ) {
				m_more.setText("--" + Integer.toString(mDispCount - SHOW_MAX_LINES) + " more--");
				m_more.setVisibility(View.VISIBLE);
			} else
				m_more.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public boolean handleKeyDown( char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, boolean bSoftInput ) {
			if( isMoreVisible() && ch == ' ' && !isLogShowing() ) {
				showLog(false);
				return true;
			}
			return false;
		}

		// ____________________________________________________________________________________
		private void updateText() {
			m_view.setText("");
			if( mDispCount > 0 ) {
				int lineCount = Math.min(SHOW_MAX_LINES, mDispCount);
				int iStart = mCurrentIdx - lineCount + 1;
				for( int i = 0; i < lineCount; i++ ) {
					LogEntry e = mLog[getIndex(iStart + i)];
					if( i > 0 )
						m_view.append("\n");
					if( e.repeat > 1 )
						m_view.append(new SpannedString(TextUtils.concat(e.msg, " (" + Integer.toString(e.repeat) + ")")));
					else
						m_view.append(e.msg);
				}
			}
		}

		// ____________________________________________________________________________________
		private Runnable mTextUpdater = new Runnable() {
			private int mLastCount = SHOW_MAX_LINES;

			public void run() {
				if( mDispCount > mLastCount ) {
					mTextUpdaterRunning = true;
					mLastCount = mDispCount;
					m_view.postDelayed(mTextUpdater, 10);
				} else {
					mTextUpdaterRunning = false;
					mLastCount = SHOW_MAX_LINES;
					updateText();
				}
			}
		};
	}
}
