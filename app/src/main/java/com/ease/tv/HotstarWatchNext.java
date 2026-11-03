package com.ease.tv;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import java.util.*;

/** Reads only Hotstar's public TV-provider rows; no account access; posters are loaded through a bounded foreground cache. */
final class HotstarWatchNext {
 static final String PKG="in.startv.hotstar";
 static final class Row {
  String id,title,episode,seasonNumber,episodeNumber,link,posterUri; Bitmap poster; long position,duration,engaged;
  Row copy(){Row r=new Row();r.id=id;r.title=title;r.episode=episode;r.seasonNumber=seasonNumber;r.episodeNumber=episodeNumber;r.link=link;r.posterUri=posterUri;r.position=position;r.duration=duration;r.engaged=engaged;return r;}
  String key(){return id+"|"+title+"|"+episode+"|"+seasonNumber+"|"+episodeNumber+"|"+link+"|"+position+"|"+duration+"|"+posterUri+"|"+(poster!=null);}
 }
 static List<Row> read(Context ctx) {
  List<Row> rows=new ArrayList<>();
  try(Cursor c=ctx.getContentResolver().query(Uri.parse("content://android.media.tv/watch_next_program"),null,null,null,null)) {
   if(c!=null){
    int id=c.getColumnIndexOrThrow("_id"),pkg=c.getColumnIndexOrThrow("package_name"),title=c.getColumnIndexOrThrow("title"),
     episode=c.getColumnIndexOrThrow("episode_title"),link=c.getColumnIndexOrThrow("intent_uri"),
     position=c.getColumnIndexOrThrow("last_playback_position_millis"),duration=c.getColumnIndexOrThrow("duration_millis"),
     engaged=c.getColumnIndexOrThrow("last_engagement_time_utc_millis"),poster=c.getColumnIndexOrThrow("poster_art_uri"),
     season=c.getColumnIndex("season_display_number"),number=c.getColumnIndex("episode_display_number");
    while(c.moveToNext()) {
     if(!WatchNextPolicy.usable(c.getString(pkg),c.getString(title),c.getString(link),c.getLong(position),c.getLong(duration)))continue;
     Row r=new Row();r.id=c.getString(id);r.title=c.getString(title);r.episode=c.getString(episode);r.link=c.getString(link);
     r.seasonNumber=season<0?null:c.getString(season);r.episodeNumber=number<0?null:c.getString(number);
     r.position=c.getLong(position);r.duration=c.getLong(duration);r.engaged=c.getLong(engaged);r.posterUri=c.getString(poster);
     if(intent(r.link)!=null)rows.add(r);
    }
   }
  } catch(RuntimeException ignored) { return Collections.emptyList(); }
  rows.sort((a,b)->Long.compare(b.engaged,a.engaged));
  return rows;
 }
 static Intent intent(String link) {
  try {
   Intent i=Intent.parseUri(link,Intent.URI_INTENT_SCHEME);
   if(i.getComponent()!=null&&!PKG.equals(i.getComponent().getPackageName()))return null;
   if(i.getPackage()!=null&&!PKG.equals(i.getPackage()))return null;
   i.setSelector(null);i.setPackage(PKG);i.setClipData(null);
   i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
   return i;
  }catch(Exception e){return null;}
 }
 static void open(Context ctx,Row row) {
  NavigationDiagnostics.event("launch_attempt",null);
  try {Intent i=intent(row.link);if(i!=null){ctx.startActivity(i);NavigationDiagnostics.event("launch_submitted",null);return;}}catch(RuntimeException ignored){NavigationDiagnostics.event("launch_error",null);}
  Intent fallback=ctx.getPackageManager().getLeanbackLaunchIntentForPackage(PKG);
  if(fallback!=null)try{ctx.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));NavigationDiagnostics.event("fallback_submitted",null);return;}catch(RuntimeException ignored){}
  NavigationDiagnostics.event("launch_unavailable",null);
 }
 static BitmapDrawable banner(Context ctx,Row r) { return banner(ctx,r,true); }
 static BitmapDrawable banner(Context ctx,Row r,boolean progress) {
  Bitmap b=Bitmap.createBitmap(480,270,Bitmap.Config.RGB_565);Canvas c=new Canvas(b);c.drawColor(Color.rgb(22,30,52));
  if(r.poster!=null){
   float scale=Math.max(480f/r.poster.getWidth(),270f/r.poster.getHeight());float w=r.poster.getWidth()*scale,h=r.poster.getHeight()*scale;
   c.drawBitmap(r.poster,null,new RectF((480-w)/2,(270-h)/2,(480+w)/2,(270+h)/2),new Paint(Paint.FILTER_BITMAP_FLAG));
   Paint shade=new Paint();shade.setShader(new LinearGradient(0,0,0,270,new int[]{0x99000000,0x55000000,0xf0000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,480,270,shade);
  }
  Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  if(progress){p.setColor(0xff46516c);c.drawRect(20,236,460,244,p);
  if(r.duration>0){p.setColor(0xff86b6ff);c.drawRect(20,236,20+440*Math.max(0f,Math.min(1f,(float)r.position/r.duration)),244,p);}
  }
  return new BitmapDrawable(ctx.getResources(),b);
 }
 private static void drawLines(Canvas c,Paint p,String s,int x,int y,int width,int max) {
  String rest=s==null?"":s;
  for(int line=0;line<max&&!rest.isEmpty();line++){
   int count=p.breakText(rest,true,width,null);if(count==0)break;
   String part=rest.substring(0,count);rest=rest.substring(count).trim();
   if(line==max-1&&!rest.isEmpty())part=part.substring(0,Math.max(0,part.length()-1))+"…";
   c.drawText(part,x,y+line*37,p);
  }
 }
 static String signature(List<Row> rows){StringBuilder s=new StringBuilder();for(Row r:rows)s.append(r.key()).append('\n');return s.toString();}
}
