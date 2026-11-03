package com.ease.tv;

/** Optional provider metadata. Missing fields must not produce invented episode or time labels. */
final class WatchDetails {
 private WatchDetails() {}
 static String summary(String season,String episode,long position,long duration) {
  String s=number(season),e=number(episode);
  String result=s.isEmpty()?"":"S"+s;
  if(!e.isEmpty())result=join(result,"E"+e);
  if(position>=1000&&duration>position){
   long remaining=duration-position;
   long minutes=remaining/60000+(remaining%60000==0?0:1);
   long hours=minutes/60,mins=minutes%60;
   String time=hours>0?hours+" hr"+(mins>0?" "+mins+" min":""):minutes+" min";
   result=join(result,time+" left");
  }
  return result;
 }
 static String text(String title,String description,String season,String episode,long position,long duration) {
  String result=summary(season,episode,position,duration);
  String detail=description==null?"":description.trim();
  if(!detail.isEmpty()&&(title==null||!detail.equalsIgnoreCase(title.trim())))
   return result.isEmpty()?detail:result+"\n"+detail;
  return result;
 }
 private static String join(String a,String b){return a.isEmpty()?b:a+" · "+b;}
 private static String number(String value){
  if(value==null)return "";
  String n=value.trim();
  return n.matches("[0-9]{1,6}")?n:"";
 }
}
