package com.ease.tv;

/** Chooses the nearest safe viewport without discarding a suitable in-flight scroll. */
final class ShelfReveal {
 static int target(int current,int planned,int viewport,int content,int left,int right){
  int max=Math.max(0,content-viewport);
  planned=Math.max(0,Math.min(max,planned));
  if(left>=planned&&right<=planned+viewport)return planned;
  int next=current;
  if(left<next)next=left;
  else if(right>next+viewport)next=right-viewport;
  return Math.max(0,Math.min(max,next));
 }
}
