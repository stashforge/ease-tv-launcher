package com.ease.tv;

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/** Explicitly opt-in local input diagnostics: no titles, links, account data or device identifiers. */
final class NavigationDiagnostics {
 static final String EXTRA="com.ease.tv.extra.NAV_DIAGNOSTICS";
 private static boolean enabled;
 private static int press;
 private NavigationDiagnostics(){}
 static void configure(Intent intent){if(intent!=null&&intent.hasExtra(EXTRA))enabled=intent.getBooleanExtra(EXTRA,false);}
 static void key(KeyEvent event,String kind,View focus){
  if(!enabled)return;
  int key=event.getKeyCode();if(key!=KeyEvent.KEYCODE_DPAD_CENTER&&key!=KeyEvent.KEYCODE_ENTER&&key!=KeyEvent.KEYCODE_NUMPAD_ENTER&&key!=KeyEvent.KEYCODE_BUTTON_A)return;
  if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)press++;
  Log.i("EaseInput","press="+press+" key="+key+" action="+event.getAction()+" time="+event.getEventTime()+" age="+(SystemClock.uptimeMillis()-event.getEventTime())+" held="+(event.getEventTime()-event.getDownTime())+" repeat="+event.getRepeatCount()+" long="+event.isLongPress()+" canceled="+event.isCanceled()+" focus="+kind+"@"+token(focus));
 }
 static void event(String action,View target){if(enabled)Log.i("EaseInput","press="+press+" outcome="+action+" time="+SystemClock.uptimeMillis()+" target="+token(target));}
 private static String token(View target){return target==null?"none":Integer.toHexString(System.identityHashCode(target));}
}
