package com.ease.tv;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.util.LruCache;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded foreground refresh: HTTPS posters cached privately, no polling or credentials. */
final class HotstarPosters {
 private static final int LIMIT=2*1024*1024;
 // Eviction only releases our reference: displayed rows may still own the bitmap.
 private static final LruCache<String,Bitmap> decoded=new LruCache<String,Bitmap>(8*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 static Bitmap load(Context ctx,String location){
  if(Thread.currentThread().isInterrupted()||location==null||location.isEmpty())return null;
  HttpURLConnection connection=null;
  try{
   Uri uri=Uri.parse(location);String scheme=uri.getScheme();byte[] bytes;
   // Content providers may replace bytes at the same URI; keep their foreground refresh behavior.
   boolean reusable="https".equals(scheme)||"android.resource".equals(scheme);
   if(reusable){Bitmap cached=decoded.get(location);if(cached!=null)return Thread.currentThread().isInterrupted()?null:cached;}
   if("content".equals(scheme)||"android.resource".equals(scheme)){
    try(InputStream in=ctx.getContentResolver().openInputStream(uri)){if(in==null)return null;bytes=PosterBytes.read(in,LIMIT);}
   }else if("https".equals(scheme)){
    File dir=new File(ctx.getCacheDir(),"hotstar-posters");if(!dir.isDirectory()&&!dir.mkdirs())return null;
    byte[] hash=MessageDigest.getInstance("SHA-256").digest(location.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder name=new StringBuilder();for(byte b:hash)name.append(String.format(java.util.Locale.ROOT,"%02x",b));
    File file=new File(dir,name.toString());
    if(file.isFile()&&file.length()<=LIMIT){try(InputStream in=new FileInputStream(file)){bytes=PosterBytes.read(in,LIMIT);}file.setLastModified(System.currentTimeMillis());}
    else{
     URL url=new URL(location);connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(1800);connection.setReadTimeout(2200);connection.setInstanceFollowRedirects(false);
     int status=connection.getResponseCode();
     // Follow only a small number of HTTPS redirects, including relative CDN redirects.
     for(int hop=0;status>=300&&status<400&&hop<2;hop++){
      if(Thread.currentThread().isInterrupted())return null;
      String redirect=connection.getHeaderField("Location");if(redirect==null)return null;
      URL next=new URL(url,redirect);if(!"https".equals(next.getProtocol()))return null;
      connection.disconnect();url=next;connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(1800);connection.setReadTimeout(2200);connection.setInstanceFollowRedirects(false);status=connection.getResponseCode();
     }
     if(status!=200||connection.getContentLengthLong()>LIMIT)return null;
     try(InputStream in=connection.getInputStream()){bytes=PosterBytes.read(in,LIMIT);}
     // Cache only valid images; a failed decoder is retried on the next foreground visit.
     Bitmap bitmap=decode(bytes);if(bitmap==null)return null;
     if(Thread.currentThread().isInterrupted())return null;
     File temp=File.createTempFile("poster-", ".tmp", dir);
     try{try(OutputStream out=new FileOutputStream(temp)){out.write(bytes);}if(!temp.renameTo(file))return null;}finally{temp.delete();}
     File[] files=dir.listFiles();if(files!=null&&files.length>10){Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-10;i++)files[i].delete();}
     if(Thread.currentThread().isInterrupted())return null;
     decoded.put(location,bitmap);
     return bitmap;
    }
   }else return null;
   if(Thread.currentThread().isInterrupted())return null;
   Bitmap bitmap=decode(bytes);
   if(Thread.currentThread().isInterrupted())return null;
   if(reusable&&bitmap!=null)decoded.put(location,bitmap);
   return bitmap;
  }catch(Exception ignored){return null;}finally{if(connection!=null)connection.disconnect();}
 }
 private static Bitmap decode(byte[] data){
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);
  if(o.outWidth<=0||o.outHeight<=0||o.outWidth>20000||o.outHeight>20000)return null;
  o.inSampleSize=1;while(o.outWidth/o.inSampleSize>960||o.outHeight/o.inSampleSize>540)o.inSampleSize*=2;
  o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(data,0,data.length,o);
 }
}
