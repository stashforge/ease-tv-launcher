package com.ease.tv;
final class WatchWindow {
 static int padding(int tiles,int columns,int existing){return Math.max(existing,Math.max(0,columns-tiles));}
 static int move(int offset,int count,int columns,int direction){
  return Math.max(0,Math.min(Math.max(0,count-columns),offset+direction));
 }
}
