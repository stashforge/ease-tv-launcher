package com.ease.tv;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Local image picker: no vendor Files app, network access, or broad file-management permission. */
public final class WallpaperImageActivity extends Activity {
 static final String SYSTEM_PICKER="system_picker";
 private final ExecutorService worker=Executors.newFixedThreadPool(2);
 private final List<Picture> pictures=new ArrayList<>();
 private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(6*1024*1024){
  @Override protected int sizeOf(String key,Bitmap image){return image.getAllocationByteCount();}
 };
 private GridView grid;private TextView status,permission;private BaseAdapter adapter;private boolean loading;
 private static final int PHOTOS=5410,COLUMNS=4;
 private static final class Picture {final Uri uri;final String name;Picture(Uri u,String n){uri=u;name=n;}}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(getResources().getFont(R.font.manrope_medium));return t;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setResult(RESULT_CANCELED);
  LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(36),dp(24),dp(36),dp(16));page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff142333,0xff090e18}));
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);page.addView(header);
  LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);header.addView(words,new LinearLayout.LayoutParams(0,-2,1));words.addView(label("Choose a wallpaper",26,0xffeff7f6));
  words.addView(label("Pictures saved on your TV · OK to import · Back to cancel",12,0xffa7bbc9));
  TextView other=button("Other picker");header.addView(other,new LinearLayout.LayoutParams(dp(132),dp(42)));other.setOnClickListener(v->{setResult(RESULT_OK,new Intent().putExtra(SYSTEM_PICKER,true));finish();});
  status=label("Loading pictures…",13,0xffb7cbd8);status.setPadding(dp(4),dp(16),0,dp(12));page.addView(status);
  permission=button("Allow access to photos");page.addView(permission,new LinearLayout.LayoutParams(dp(260),dp(44)));permission.setVisibility(View.GONE);permission.setOnClickListener(v->requestPhotos());
  grid=new GridView(this);grid.setNumColumns(COLUMNS);grid.setHorizontalSpacing(dp(16));grid.setVerticalSpacing(dp(12));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setPadding(dp(5),dp(5),dp(5),dp(8));grid.setClipToPadding(false);grid.setVerticalScrollBarEnabled(false);page.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
  adapter=new BaseAdapter(){
   public int getCount(){return pictures.size();}public Object getItem(int p){return pictures.get(p);}public long getItemId(int p){return p;}
   public View getView(int p,View old,ViewGroup parent){
    Picture picture=pictures.get(p);LinearLayout tile;ImageView image;TextView title;
    if(old==null){tile=new LinearLayout(WallpaperImageActivity.this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(5),dp(5),dp(5),dp(4));tile.setBackgroundResource(R.drawable.input_row_bg);tile.setDuplicateParentStateEnabled(true);
     image=new ImageView(WallpaperImageActivity.this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable shape=new GradientDrawable();shape.setColor(0xff213747);shape.setCornerRadius(dp(10));image.setBackground(shape);image.setOutlineProvider(ViewOutlineProvider.BACKGROUND);image.setClipToOutline(true);image.setDuplicateParentStateEnabled(true);
     int width=(getResources().getDisplayMetrics().widthPixels-dp(72+10+16*(COLUMNS-1)))/COLUMNS-dp(10);tile.addView(image,new LinearLayout.LayoutParams(-1,Math.round(width*9f/16f)));
     title=label("",12,0xffe4edf5);title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
    }else{tile=(LinearLayout)old;image=(ImageView)tile.getChildAt(0);title=(TextView)tile.getChildAt(1);}
    title.setText(picture.name);String key=picture.uri.toString();image.setTag(key);image.setImageBitmap(cache.get(key));
    if(cache.get(key)==null)worker.submit(()->{Bitmap bitmap=null;try{if(Build.VERSION.SDK_INT>=29)bitmap=getContentResolver().loadThumbnail(picture.uri,new Size(400,225),null);else bitmap=legacyThumbnail(picture.uri);}catch(Exception ignored){}if(bitmap==null)return;final Bitmap result=bitmap;cache.put(key,result);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&key.equals(image.getTag()))image.setImageBitmap(result);});});
    return tile;
   }
  };grid.setAdapter(adapter);grid.setOnItemClickListener((p,v,pos,id)->{setResult(RESULT_OK,new Intent().setData(pictures.get(pos).uri));finish();});
  grid.setOnKeyListener((v,key,event)->{int from=grid.getSelectedItemPosition();if(from<0)return false;int target=from;if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&from%4==3)target=Math.min(pictures.size()-1,from+1);else if(key==KeyEvent.KEYCODE_DPAD_LEFT&&from%4==0&&from>0)target=from-1;else return false;if(event.getAction()==KeyEvent.ACTION_DOWN)grid.setSelection(target);return true;});
  setContentView(page);if(hasPhotos())loadPictures();else requestPhotos();
 }
 private TextView button(String value){TextView t=label(value,13,0xffedf5fc);t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setClickable(true);t.setBackgroundResource(R.drawable.input_row_bg);t.setOnFocusChangeListener((v,f)->t.setTextColor(f?0xff142333:0xffedf5fc));return t;}
 private boolean hasPhotos(){return checkSelfPermission(Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED||(Build.VERSION.SDK_INT>=34&&checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==PackageManager.PERMISSION_GRANTED);}
 private void requestPhotos(){String[] requested=Build.VERSION.SDK_INT>=34?new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED}:new String[]{Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_EXTERNAL_STORAGE};try{requestPermissions(requested,PHOTOS);}catch(RuntimeException unavailable){showPermission();}}
 @Override public void onRequestPermissionsResult(int code,String[] names,int[] grants){super.onRequestPermissionsResult(code,names,grants);if(code==PHOTOS){if(hasPhotos())loadPictures();else showPermission();}}
 private void showPermission(){status.setText("Allow photo access to browse local pictures, or choose Other picker.");permission.setVisibility(View.VISIBLE);permission.requestFocus();}
 private void loadPictures(){
  if(loading)return;loading=true;permission.setVisibility(View.GONE);status.setText("Loading pictures…");worker.submit(()->{
   List<Picture> found=new ArrayList<>();String error=null;
   try(Cursor cursor=getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME},null,null,MediaStore.Images.Media.DATE_ADDED+" DESC")){
    if(cursor!=null)while(cursor.moveToNext()&&found.size()<1000){found.add(new Picture(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(0)),cursor.getString(1)));}
   }catch(RuntimeException e){error="Pictures could not be loaded. Try Other picker.";}
   final String message=error;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;loading=false;pictures.clear();pictures.addAll(found);adapter.notifyDataSetChanged();status.setText(message!=null?message:found.isEmpty()?"No pictures found. Copy images to Downloads or Pictures, then reopen this page.":found.size()+" picture"+(found.size()==1?"":"s")+" · Select one to import");if(!found.isEmpty()){grid.requestFocus();grid.setSelection(0);}});
  });
 }
 private Bitmap legacyThumbnail(Uri uri)throws java.io.IOException{
  BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(java.io.InputStream stream=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(stream,null,options);}int sample=1;while(options.outWidth/sample>800||options.outHeight/sample>450)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;try(java.io.InputStream stream=getContentResolver().openInputStream(uri)){return BitmapFactory.decodeStream(stream,null,options);}
 }
 @Override protected void onDestroy(){worker.shutdownNow();cache.evictAll();super.onDestroy();}
}
