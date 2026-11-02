package com.ease.tv;
import java.io.*;
final class PosterBytes {
 static byte[] read(InputStream input,int max)throws IOException{
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n,total=0;
  while((n=input.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException();if(n>max-total)throw new IOException("Thumbnail exceeds size limit");total+=n;out.write(buffer,0,n);}return out.toByteArray();
 }
}
