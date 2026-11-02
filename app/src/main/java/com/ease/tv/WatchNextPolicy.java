package com.ease.tv;
final class WatchNextPolicy {
 static int focusable(int position, int start, int count, int columns) {
  return count>0 && position>=start && position<start+columns ? Math.min(position,start+count-1) : position;
 }
 static boolean usable(String pkg, String title, String link, long position, long duration) {
  return "in.startv.hotstar".equals(pkg) && title != null && !title.trim().isEmpty()
   && link != null && !link.trim().isEmpty() && (duration <= 0 || position < duration);
 }
}
