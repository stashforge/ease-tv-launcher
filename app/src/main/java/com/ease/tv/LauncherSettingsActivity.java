package com.ease.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.io.File;
import java.util.*;

/** Settings for the personal two-shelf Home layout. */
public final class LauncherSettingsActivity extends Activity {
 static final String EXTRA_REORDER="com.ease.tv.extra.REORDER_FROM_SETTINGS";
 private SharedPreferences choices;
 private TextView wallpaperValue,motionValue,clockValue;
 private Switch animationsSwitch;
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(LauncherLocale.wrap(base));}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);choices=getSharedPreferences(Config.SETTINGS_NAME,MODE_PRIVATE);
  LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.HORIZONTAL);page.setPadding(dp(40),dp(32),dp(40),dp(24));page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff141d30,0xff090e18}));
  LinearLayout intro=new LinearLayout(this);intro.setOrientation(LinearLayout.VERTICAL);intro.setPadding(0,dp(14),dp(36),0);page.addView(intro,new LinearLayout.LayoutParams(dp(280),-1));
  TextView kicker=text("MAKE IT YOURS",12,0xff8ca9dc);intro.addView(kicker);
  TextView title=text("Launcher\nsettings",34,0xfff1f4fb);LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,-2);titleLp.topMargin=dp(16);intro.addView(title,titleLp);
  TextView description=text("A few choices for your Home screen. Changes are saved automatically.",15,0xffaab7cf);description.setLineSpacing(dp(5),1);LinearLayout.LayoutParams descLp=new LinearLayout.LayoutParams(-1,-2);descLp.topMargin=dp(20);intro.addView(description,descLp);
  ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(false);page.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(6),dp(6),dp(6),dp(6));scroll.addView(list);
  section(list,"APPEARANCE");
  wallpaperValue=option(list,"Wallpaper","Choose a picture or the default gradient",R.drawable.ic_home_settings_pill,()->showWallpaperPicker());
  motionValue=option(list,"Animations","Smooth movement and focus transitions",R.drawable.ic_swap_vert,()->{Config c=Config.load(this);choices.edit().putBoolean("animations",!c.animations).apply();changed();refresh();});
  animationsSwitch=new Switch(this);animationsSwitch.setShowText(false);animationsSwitch.setFocusable(false);animationsSwitch.setClickable(false);animationsSwitch.setContentDescription("Animations");
  animationsSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(0xfff1f5ff));animationsSwitch.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff548eff,0xff657086}));
  motionValue.setVisibility(View.GONE);((LinearLayout)motionValue.getParent()).addView(animationsSwitch,new LinearLayout.LayoutParams(dp(50),dp(36)));
  clockValue=option(list,"Time format","Use your preferred clock format",R.drawable.ic_settings,()->showTimePicker());
  section(list,"TV & LAUNCHER");
  option(list,"TV settings","Sound, picture, network and connected devices",R.drawable.ic_settings,()->open(SystemSettings.intent(this,Settings.ACTION_SETTINGS)));
  option(list,"Default launcher","Manage which Home screen the TV opens",R.drawable.ic_apps_pill,()->open(new Intent(this,HomeSettingsActivity.class)));
  setContentView(page);refresh();list.getChildAt(1).requestFocus();
 }
 private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(getResources().getFont(R.font.manrope_medium));return t;}
 private void section(LinearLayout parent,String name){TextView t=text(name,11,0xff8da4ca);t.setLetterSpacing(0.12f);t.setPadding(dp(12),dp(16),0,dp(10));parent.addView(t);}
 private TextView option(LinearLayout parent,String title,String detail,int icon,Runnable action){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(12),dp(16),dp(12));row.setFocusable(true);row.setClickable(true);row.setBackgroundResource(R.drawable.input_row_bg);
  ImageView image=new ImageView(this);image.setImageResource(icon);image.setColorFilter(0xffb6c9e9);LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(dp(22),dp(22));iconLp.rightMargin=dp(16);row.addView(image,iconLp);
  LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);TextView label=text(title,17,0xffedf2fc);TextView hint=text(detail,11,0xffa4b2ca);hint.setSingleLine(true);hint.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(label);words.addView(hint);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
  TextView value=text("›",12,0xffbdcce4);value.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams valueLp=new LinearLayout.LayoutParams(-2,-2);valueLp.leftMargin=dp(12);row.addView(value,valueLp);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76));lp.bottomMargin=dp(6);parent.addView(row,lp);
  row.setOnFocusChangeListener((v,focus)->{label.setTextColor(focus?0xff111827:0xffedf2fc);hint.setTextColor(focus?0xff4b5870:0xffa4b2ca);value.setTextColor(focus?0xff263650:0xffbdcce4);image.setColorFilter(focus?0xff263650:0xffb6c9e9);});row.setOnClickListener(v->action.run());return value;
 }
 private void refresh(){Config c=Config.load(this);String wallpaper=choices.getString(Config.WALLPAPER_CHOICE_KEY,null);wallpaperValue.setText("none".equals(wallpaper)?"Gradient":wallpaper==null?"Default":"Picture");animationsSwitch.setChecked(c.animations);clockValue.setText("12".equals(c.clockFormat)?"12-hour":"24".equals(c.clockFormat)?"24-hour":"Automatic");}
 private void changed(){setResult(RESULT_OK);choices.edit().putLong(Config.SETTINGS_REVISION_KEY,choices.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();}
 private static int indexOf(List<File> images,String name){if(name==null)return -1;for(int i=0;i<images.size();i++)if(images.get(i).getName().equals(name))return i;return -1;}
 private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(this,"Not available",Toast.LENGTH_SHORT).show();}}
 private final java.util.concurrent.ExecutorService thumbnails=java.util.concurrent.Executors.newSingleThreadExecutor();
 private LinearLayout pickerPanel(String title,String subtitle){
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(20),dp(22),dp(16));
  GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff202b42,0xff101725});background.setCornerRadius(dp(22));background.setStroke(dp(1),0xff4b5870);panel.setBackground(background);
  panel.addView(text(title,25,0xfff1f5ff));TextView hint=text(subtitle,12,0xffaabbd8);LinearLayout.LayoutParams hintLp=new LinearLayout.LayoutParams(-1,-2);hintLp.topMargin=dp(6);hintLp.bottomMargin=dp(16);panel.addView(hint,hintLp);return panel;
 }
 private android.app.Dialog pickerDialog(LinearLayout panel,int width){
  android.app.Dialog dialog=new android.app.Dialog(this);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(panel);Window window=dialog.getWindow();window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams params=window.getAttributes();params.dimAmount=0.65f;window.setAttributes(params);dialog.setOnShowListener(v->window.setLayout(dp(width),-2));return dialog;
 }
 private void showTimePicker(){
  LinearLayout panel=pickerPanel("Time format","Choose how the clock appears on Home");android.app.Dialog dialog=pickerDialog(panel,480);
  String current=Config.load(this).clockFormat;String[] modes={"system","12","24"},names={"Automatic","12-hour","24-hour"},descriptions={"Follow the TV’s clock preference","02:30 PM","14:30"};View first=null,selected=null;
  for(int i=0;i<3;i++){final String mode=modes[i];TextView check=option(panel,names[i],descriptions[i],android.R.drawable.ic_menu_recent_history,()->{choices.edit().putString(Config.CLOCK_FORMAT_KEY,mode).apply();changed();refresh();dialog.dismiss();});check.setText(mode.equals(current)?"✓":"");View row=panel.getChildAt(panel.getChildCount()-1);if(first==null)first=row;if(mode.equals(current))selected=row;}
  TextView foot=text("OK to select · Back to close",11,0xff91a5c7);foot.setPadding(dp(12),dp(10),0,0);panel.addView(foot);dialog.show();(selected==null?first:selected).requestFocus();
 }
 private void showWallpaperPicker(){
  File dir=Config.dir(this);List<File> images=new ArrayList<>(BundledWallpapers.images(this));images.addAll(WallpaperRotation.images(new File(getFilesDir(),"imported-wallpapers")));if(dir!=null){File single=new File(dir,Config.WALLPAPER_NAME);if(single.isFile())images.add(single);images.addAll(WallpaperRotation.images(new File(dir,"wallpapers")));}
  LinearLayout panel=pickerPanel("Wallpaper","Choose a background · Pictures are available offline");android.app.Dialog dialog=pickerDialog(panel,680);
  ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(3);grid.setPadding(dp(4),dp(4),dp(4),dp(4));scroll.addView(grid);panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(320)));
  List<File> tiles=new ArrayList<>();tiles.add(null);tiles.add(null);tiles.addAll(images);String current=choices.getString(Config.WALLPAPER_CHOICE_KEY,null);View focus=null;
  for(int i=0;i<tiles.size();i++){
   final int index=i;File file=tiles.get(i);String key=i==0?null:i==1?"none":file.getName();String name=i==0?"Theme default":i==1?"Gradient":file.getName().replaceFirst("\\.[^.]+$","").replace('-',' ');boolean active=Objects.equals(current,key);
   LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(6),dp(6),dp(6),dp(6));tile.setFocusable(true);tile.setClickable(true);tile.setBackgroundResource(R.drawable.input_row_bg);
   ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TL_BR,i==1?new int[]{0xff2e4166,0xff171426}:new int[]{0xff60789a,0xff243249});shape.setCornerRadius(dp(10));preview.setBackground(shape);preview.setOutlineProvider(ViewOutlineProvider.BACKGROUND);preview.setClipToOutline(true);tile.addView(preview,new LinearLayout.LayoutParams(-1,dp(101)));
   TextView caption=text((active?"✓  ":"")+name,12,0xffeaf0fc);caption.setGravity(Gravity.CENTER);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(caption,new LinearLayout.LayoutParams(-1,dp(30)));
   tile.setOnFocusChangeListener((v,focused)->caption.setTextColor(focused?0xff132038:0xffeaf0fc));tile.setOnClickListener(v->{if(index==0)choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();else choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,key).apply();changed();refresh();dialog.dismiss();});
   tile.setOnLongClickListener(v->{if(file!=null&&file.getParentFile().equals(new File(getFilesDir(),"imported-wallpapers")))showRemoveWallpaper(file,dialog);return true;});
   android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();lp.width=dp(200);lp.height=dp(145);lp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,lp);
   tile.setOnKeyListener((view,keyCode,event)->{
    if(keyCode!=KeyEvent.KEYCODE_DPAD_LEFT&&keyCode!=KeyEvent.KEYCODE_DPAD_RIGHT&&keyCode!=KeyEvent.KEYCODE_DPAD_UP&&keyCode!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
    if(event.getAction()!=KeyEvent.ACTION_DOWN)return true;
    int from=grid.indexOfChild(view),count=grid.getChildCount(),target=from;
    if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)target=Math.min(count-1,from+1);
    else if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT)target=Math.max(0,from-1);
    else if(keyCode==KeyEvent.KEYCODE_DPAD_UP){if(from>=3)target=from-3;}
    else if(from/3<(count-1)/3)target=Math.min(count-1,from+3);else{View importer=panel.findViewWithTag("import-wallpaper");if(importer!=null)importer.requestFocus();return true;}
    if(target!=from)grid.getChildAt(target).requestFocus();return true;
   });
   if(active)focus=tile;
   File imageFile=file;if(index==0){Config config=Config.load(this);if(!config.wallpapers.isEmpty())imageFile=config.wallpapers.get(0);}
   if(imageFile!=null){final File image=imageFile;android.graphics.Bitmap cached=WallpaperPreviews.get(image);if(cached!=null)preview.setImageBitmap(cached);else thumbnails.submit(()->{android.graphics.Bitmap bitmap=WallpaperPreviews.load(image);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&dialog.isShowing()&&bitmap!=null)preview.setImageBitmap(bitmap);});});}
  }
  TextView importButton=text("+  Add from storage",14,0xffedf2fc);importButton.setTag("import-wallpaper");importButton.setOnKeyListener((v,k,e)->{if(k==android.view.KeyEvent.KEYCODE_DPAD_UP){if(e.getAction()==android.view.KeyEvent.ACTION_DOWN&&grid.getChildCount()>0)grid.getChildAt(grid.getChildCount()-1).requestFocus();return true;}return false;});importButton.setGravity(Gravity.CENTER_VERTICAL);importButton.setPadding(dp(12),0,dp(12),0);importButton.setBackgroundResource(R.drawable.input_row_bg);importButton.setFocusable(true);importButton.setClickable(true);importButton.setOnFocusChangeListener((v,focusImport)->importButton.setTextColor(focusImport?0xff132038:0xffedf2fc));importButton.setOnClickListener(v->{dialog.dismiss();chooseImage();});panel.addView(importButton,new LinearLayout.LayoutParams(-1,dp(40)));
  TextView foot=text("OK to apply · Hold an imported picture to remove · Back to close",11,0xff91a5c7);foot.setPadding(dp(8),dp(12),0,0);panel.addView(foot);dialog.show();if(focus!=null)focus.requestFocus();else grid.getChildAt(0).requestFocus();
 }
 private void showRemoveWallpaper(File file,android.app.Dialog wallpaperDialog){
  LinearLayout panel=pickerPanel("Remove wallpaper?","This removes the imported copy from the launcher. The original picture is kept.");android.app.Dialog confirm=pickerDialog(panel,480);
  option(panel,"Remove wallpaper",file.getName(),android.R.drawable.ic_menu_delete,()->{
   if(!file.getParentFile().equals(new File(getFilesDir(),"imported-wallpapers"))||!file.delete()){Toast.makeText(this,"Could not remove wallpaper",Toast.LENGTH_SHORT).show();confirm.dismiss();return;}
   WallpaperPreviews.remove(file);
   if(file.getName().equals(choices.getString(Config.WALLPAPER_CHOICE_KEY,null)))choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,"none").apply();changed();refresh();confirm.dismiss();wallpaperDialog.dismiss();showWallpaperPicker();
  });
  option(panel,"Cancel","Keep this wallpaper",android.R.drawable.ic_menu_close_clear_cancel,confirm::dismiss);confirm.show();panel.getChildAt(panel.getChildCount()-1).requestFocus();
 }
 private static final int PICK_IMAGE=5408;
 private void chooseImage(){startActivityForResult(new Intent(this,WallpaperImageActivity.class),PICK_IMAGE);}
 private void chooseSystemImage(){
  Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
  android.content.pm.ResolveInfo handler=getPackageManager().resolveActivity(intent,0);
  if(handler==null||handler.activityInfo.packageName.contains("frameworkpackagestubs")){
   intent=new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
   List<android.content.pm.ResolveInfo> pickers=getPackageManager().queryIntentActivities(intent,0);handler=null;
   for(android.content.pm.ResolveInfo candidate:pickers)if(!candidate.activityInfo.packageName.contains("frameworkpackagestubs")){handler=candidate;break;}
   if(handler==null){Toast.makeText(this,"No image picker is installed on this TV",Toast.LENGTH_LONG).show();return;}
   intent.setComponent(new android.content.ComponentName(handler.activityInfo.packageName,handler.activityInfo.name));
  }
  try{startActivityForResult(intent,PICK_IMAGE);}catch(RuntimeException e){Toast.makeText(this,"Could not open image picker",Toast.LENGTH_LONG).show();}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);if(request==PICK_IMAGE&&result==RESULT_OK&&data!=null&&data.getBooleanExtra(WallpaperImageActivity.SYSTEM_PICKER,false)){chooseSystemImage();return;}if(request!=PICK_IMAGE||result!=RESULT_OK||data==null||data.getData()==null)return;
  final android.net.Uri uri=data.getData();Toast.makeText(this,"Importing wallpaper…",Toast.LENGTH_SHORT).show();
  thumbnails.submit(()->{
   File temporary=null;
   try{
    File directory=new File(getFilesDir(),"imported-wallpapers");if(!directory.isDirectory()&&!directory.mkdirs())throw new java.io.IOException("Storage unavailable");
    temporary=File.createTempFile("wallpaper-",".tmp",directory);
    try(java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.OutputStream out=new java.io.FileOutputStream(temporary)){
     if(in==null)throw new java.io.IOException("Cannot read image");byte[] buffer=new byte[32768];int count,total=0;while((count=in.read(buffer))!=-1){total+=count;if(total>WallpaperRotation.MAX_IMAGE_BYTES)throw new java.io.IOException("Image exceeds 32 MB");out.write(buffer,0,count);}
    }
    android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(temporary.getPath(),bounds);
    if(bounds.outWidth<1||bounds.outHeight<1||bounds.outWidth>16384||bounds.outHeight>16384||(long)bounds.outWidth*bounds.outHeight>80000000)throw new java.io.IOException("Unsupported image dimensions");
    String extension="image/jpeg".equals(bounds.outMimeType)?".jpg":"image/png".equals(bounds.outMimeType)?".png":"image/webp".equals(bounds.outMimeType)?".webp":null;
    if(extension==null)throw new java.io.IOException("Choose a JPEG, PNG or WebP image");
    String label="Wallpaper";
    try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())label=cursor.getString(0);}catch(RuntimeException ignored){}
    if(label==null)label="Wallpaper";label=label.replaceFirst("\\.[^.]+$","").replaceAll("[^a-zA-Z0-9 _-]","_");if(label.length()>60)label=label.substring(0,60);
    File image=new File(directory,label+"-"+System.currentTimeMillis()+extension);if(!temporary.renameTo(image))throw new java.io.IOException("Could not save image");
    runOnUiThread(()->{if(isFinishing()||isDestroyed())return;choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,image.getName()).apply();changed();refresh();Toast.makeText(this,"Wallpaper imported and selected",Toast.LENGTH_SHORT).show();showWallpaperPicker();});
   }catch(Exception e){if(temporary!=null)temporary.delete();String message=e.getMessage();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())Toast.makeText(this,"Import failed: "+message,Toast.LENGTH_LONG).show();});}
  });
 }
 @Override protected void onDestroy(){thumbnails.shutdownNow();super.onDestroy();}
}
