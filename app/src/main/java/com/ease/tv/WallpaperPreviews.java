package com.ease.tv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.File;

/** Process-wide, bounded thumbnails only; never holds an Activity, View, or full-size wallpaper. */
final class WallpaperPreviews {
 private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 private WallpaperPreviews(){}
 private static String key(File file){return file.getAbsolutePath()+"\n"+file.length()+":"+file.lastModified();}
 static Bitmap get(File file){return file.isFile()?CACHE.get(key(file)):null;}
 /** Called on the existing thumbnail worker. Metadata changes select a fresh thumbnail automatically. */
 static Bitmap load(File file){
  if(!file.isFile())return null;String key=key(file);Bitmap existing=CACHE.get(key);if(existing!=null)return existing;
  BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),bounds);
  if(bounds.outWidth<1||bounds.outHeight<1)return null;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
  while(bounds.outWidth/options.inSampleSize>512||bounds.outHeight/options.inSampleSize>288)options.inSampleSize*=2;
  Bitmap bitmap=BitmapFactory.decodeFile(file.getPath(),options);
  // A file may be replaced or removed while decoding. Never cache that obsolete result.
  if(bitmap!=null&&file.isFile()&&key.equals(key(file)))CACHE.put(key,bitmap);
  return bitmap;
 }
 static void remove(File file){String prefix=file.getAbsolutePath()+"\n";for(String key:CACHE.snapshot().keySet())if(key.startsWith(prefix))CACHE.remove(key);}
}
