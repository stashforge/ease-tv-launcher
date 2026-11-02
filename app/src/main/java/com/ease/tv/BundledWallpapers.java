package com.ease.tv;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Original image files bundled for offline use; copied once to the local wallpaper decoder's file interface. */
final class BundledWallpapers {
 private static final String[] NAMES={"Blue evening.png","Mountain light.jpg","Sunset clouds.jpg"};
 static synchronized List<File> images(Context context){
  File directory=new File(context.getFilesDir(),"bundled-wallpapers");
  if(!directory.isDirectory()&&!directory.mkdirs())return Collections.emptyList();
  List<File> result=new ArrayList<>();
  for(String name:NAMES){File file=new File(directory,name);
   if(!file.isFile()){
    File temporary=new File(directory,name+".tmp");
    try(InputStream in=context.getAssets().open("wallpapers/"+name);OutputStream out=new FileOutputStream(temporary)){
     byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
    }catch(IOException e){temporary.delete();continue;}
    if(!temporary.renameTo(file)){temporary.delete();continue;}
   }
   result.add(file);
  }
  return result;
 }
}
