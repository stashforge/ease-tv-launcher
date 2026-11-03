package com.ease.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Installed apps live here; pinning changes only Home shortcuts, never installation. */
public final class AppCatalogActivity extends Activity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final List<App> apps=new ArrayList<>();
 private static final int COLUMNS=5;
 private GridView grid;private boolean selecting;private Config config;private BaseAdapter adapter;
 private Set<String> additions,removed;
 private boolean navigating;
 private int navigationTarget=-1;
 // Kept until the selected view is laid out; GridView selection is not synchronous.
 private long focusDuration=120;
 private boolean repeatNavigation;
 private Runnable navigationFinish,navigationSettled;
 private static final class App {String label,pkg;ComponentName component;Drawable icon,banner;}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);selecting=getIntent().getBooleanExtra("select",false);config=Config.load(this);additions=new HashSet<>(LibraryApps.load(this));removed=new HashSet<>(LibraryApps.loadRemoved(this));setResult(RESULT_OK);
  FrameLayout page=new FrameLayout(this);page.setBackgroundColor(0xff0b101a);
  grid=new GridView(this);grid.setNumColumns(COLUMNS);grid.setHorizontalSpacing(dp(30));grid.setVerticalSpacing(dp(14));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setClipToPadding(false);grid.setClipChildren(false);grid.setPadding(dp(40),dp(104),dp(40),dp(12));page.setClipChildren(false);grid.setVerticalScrollBarEnabled(false);grid.setHorizontalScrollBarEnabled(false);
  adapter=new BaseAdapter(){
   public int getCount(){return apps.size();} public Object getItem(int p){return apps.get(p);}public long getItemId(int p){return apps.get(p).pkg.hashCode();}
   public View getView(int p,View old,ViewGroup parent){
    App app=apps.get(p);
    LinearLayout tile=new LinearLayout(AppCatalogActivity.this){
     @Override public void setSelected(boolean selected){boolean changed=selected!=isSelected();super.setSelected(selected);if(changed)animate().scaleX(selected?1.2f:1f).scaleY(selected?1.2f:1f).setDuration(config.animations?focusDuration:0).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
    };tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(4),dp(6),dp(4),dp(6));
    int artWidth=(getResources().getDisplayMetrics().widthPixels-dp(80)-dp(30)*(COLUMNS-1))/COLUMNS-dp(8);
    int artHeight=Math.round(artWidth*9f/16f);
    FrameLayout art=new FrameLayout(AppCatalogActivity.this);art.setDuplicateParentStateEnabled(true);
    art.setPadding(0,0,0,0);art.setClipChildren(false);
    art.setForeground(new SelectionRim(getResources().getDisplayMetrics().density));
    ImageView image=new ImageView(AppCatalogActivity.this);image.setImageDrawable(app.banner!=null?app.banner:app.icon);image.setScaleType(app.banner!=null?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.FIT_CENTER);
    GradientDrawable imageShape=new GradientDrawable();imageShape.setColor(0xff171b24);imageShape.setCornerRadius(13.5f*getResources().getDisplayMetrics().density);image.setBackground(imageShape);image.setOutlineProvider(ViewOutlineProvider.BACKGROUND);image.setClipToOutline(true);
    if(app.banner==null)image.setPadding(dp(24),dp(12),dp(24),dp(12));
    art.addView(image,new FrameLayout.LayoutParams(-1,-1));tile.addView(art,new LinearLayout.LayoutParams(-1,artHeight));
    TextView label=new TextView(AppCatalogActivity.this);label.setText((home(app.pkg)?"✓  ":"")+app.label);label.setTextColor(Color.WHITE);label.setTextSize(14);label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
    LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-1,dp(32));labelParams.topMargin=dp(2);tile.addView(label,labelParams);
    tile.setLayoutParams(new AbsListView.LayoutParams(-1,artHeight+dp(46)));return tile;
   }
  };
  grid.setSelector(new ColorDrawable(Color.TRANSPARENT));grid.setAdapter(adapter);grid.setOnItemClickListener((p,v,at,id)->{if(selecting){if(addOnce(at))finish();}else launch(at);});grid.setOnItemLongClickListener((p,v,at,id)->{if(selecting)addOnce(at);else appMenu(v,at);return true;});page.addView(grid,new FrameLayout.LayoutParams(-1,-1));
  grid.setOnKeyListener((view,key,event)->{
   if(key!=KeyEvent.KEYCODE_DPAD_RIGHT&&key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
   if(event.getAction()!=KeyEvent.ACTION_DOWN||apps.isEmpty())return true;
   repeatNavigation=event.getRepeatCount()>0;
   focusDuration=repeatNavigation?70:120;
   int current=navigationTarget>=0?navigationTarget:Math.max(0,grid.getSelectedItemPosition()),target=current;
   if(key==KeyEvent.KEYCODE_DPAD_RIGHT)target=Math.min(apps.size()-1,current+1);
   else if(key==KeyEvent.KEYCODE_DPAD_LEFT)target=Math.max(0,current-1);
   else if(key==KeyEvent.KEYCODE_DPAD_UP)target=current>=COLUMNS?current-COLUMNS:current;
   else target=current/COLUMNS<(apps.size()-1)/COLUMNS?Math.min(apps.size()-1,current+COLUMNS):current;
   if(target!=current){
    navigationTarget=target;
    if(!navigating)moveSelection(target);
   }
   return true;
  });
  LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);heading.setPadding(dp(40),dp(24),dp(40),0);heading.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  TextView title=new TextView(this);title.setText(selecting?"Add apps to Home":"Apps");title.setTextColor(Color.WHITE);title.setTextSize(28);heading.addView(title);
  TextView description=new TextView(this);description.setText(selecting?"OK to add and return · Hold OK to add more":"OK to open · Hold OK for app options");description.setTextColor(0xffaab2c2);description.setTextSize(14);heading.addView(description);
  page.addView(heading,new FrameLayout.LayoutParams(-1,dp(104)));
  grid.setOnScrollListener(new AbsListView.OnScrollListener(){
   public void onScrollStateChanged(AbsListView view,int state){}
   public void onScroll(AbsListView view,int first,int visible,int total){
    View child=view.getChildAt(0);if(child==null)return;
    int rowStride=child.getHeight()+grid.getVerticalSpacing();
    int offset=Math.max(0,(first/COLUMNS)*rowStride+grid.getPaddingTop()-child.getTop());
    heading.setTranslationY(-offset);
    heading.setVisibility(offset>=dp(104)?View.INVISIBLE:View.VISIBLE);
   }
  });setContentView(page);
  worker.submit(()->{
   PackageManager pm=getPackageManager();List<ResolveInfo> found=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0);found.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0));Set<String> seen=new HashSet<>();List<App> loaded=new ArrayList<>();
   for(ResolveInfo ri:found){String pkg=ri.activityInfo.packageName;String lower=pkg.toLowerCase(Locale.ROOT);if(!ri.activityInfo.exported||pkg.equals(getPackageName())||config.libraryExcludedApps.contains(pkg)||!seen.add(pkg)||lower.contains("factory")||lower.contains("production")||lower.contains("fac.menu")||lower.startsWith("com.at."))continue;App a=new App();a.pkg=pkg;a.component=new ComponentName(pkg,ri.activityInfo.name);a.label=String.valueOf(ri.loadLabel(pm));a.icon=ri.loadIcon(pm);a.banner=ri.activityInfo.loadBanner(pm);if(a.banner==null)a.banner=ri.activityInfo.applicationInfo.loadBanner(pm);loaded.add(a);}
   loaded.sort((a,b)->a.label.compareToIgnoreCase(b.label));runOnUiThread(()->{if(isFinishing()||isDestroyed())return;apps.addAll(loaded);adapter.notifyDataSetChanged();grid.requestFocus();});
  });
 }
 private void moveSelection(int target){
  View first=grid.getChildAt(0);if(first==null)return;
  int firstRow=grid.getFirstVisiblePosition()/COLUMNS;
  int rowHeight=first.getHeight()+grid.getVerticalSpacing();
  int top=first.getTop()+(target/COLUMNS-firstRow)*rowHeight;
  int desired=Math.max(dp(12),Math.min(top,grid.getHeight()-first.getHeight()-dp(20)));
  // Restore the heading when returning to the first row.
  if(target<COLUMNS)desired=grid.getPaddingTop();
  final int destination=desired;
  int distance=top-destination;
  if(distance==0){grid.setSelection(target);navigationTarget=-1;return;}
  if(!config.animations){grid.setSelectionFromTop(target,destination);navigationTarget=-1;return;}
  navigating=true;
  int duration=repeatNavigation?100:180;
  grid.smoothScrollBy(distance,duration);
  navigationFinish=()->{
   navigationFinish=null;if(isFinishing()||isDestroyed())return;
   grid.setSelection(target);
   // Wait for layout before measuring another row. Coalesce into one destination,
   // rather than dropping DPAD presses or replaying an unbounded animation queue.
   navigationSettled=()->{
    navigationSettled=null;if(isFinishing()||isDestroyed())return;
    navigating=false;
    int next=navigationTarget;
    if(next>=0&&next!=target)moveSelection(next);
    else navigationTarget=-1;
   };
   grid.postOnAnimation(navigationSettled);
  };
  grid.postDelayed(navigationFinish,duration+20);
 }
 private boolean home(String pkg){int at=config.dockApps.indexOf(pkg);return !removed.contains(pkg)&&((at>=0&&at<config.columns)||additions.contains(pkg));}
 private boolean addOnce(int at){
  if(home(apps.get(at).pkg)){Toast.makeText(this,"Already on Home",Toast.LENGTH_SHORT).show();return true;}
  return savePin(at,true);
 }
 private boolean savePin(int at,boolean add){
  App app=apps.get(at);
  if(!LibraryApps.save(this,app.pkg,add)){Toast.makeText(this,"Could not update Home",Toast.LENGTH_SHORT).show();return false;}
  additions=new HashSet<>(LibraryApps.load(this));removed=new HashSet<>(LibraryApps.loadRemoved(this));adapter.notifyDataSetChanged();
  Toast.makeText(this,add?"Added to Home":"Removed from Home",Toast.LENGTH_SHORT).show();return true;
 }
 private void appMenu(View anchor,int at){
  App app=apps.get(at);boolean pinned=home(app.pkg);
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(6),dp(8),dp(6),dp(6));
  GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xfa292d38,0xfa171b24});background.setCornerRadius(dp(18));background.setStroke(dp(1),0x665d6a82);panel.setBackground(background);
  TextView name=new TextView(this);name.setText(app.label);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setTextSize(12);name.setTextColor(0xffaebbd1);name.setPadding(dp(12),dp(2),dp(12),dp(10));panel.addView(name);
  int width=dp(224);PopupWindow popup=new PopupWindow(panel,width,-2,true);popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));popup.setOutsideTouchable(true);popup.setElevation(dp(16));popup.setAnimationStyle(0);
  final boolean[] openingOk={true};
  String[] labels={pinned?"Remove from Home":"Add to Home","App info","Open"};
  int[] icons={R.drawable.ic_home_settings_pill,android.R.drawable.ic_menu_info_details,R.drawable.ic_apps_pill};
  for(int i=0;i<labels.length;i++){
   final int action=i;TextView row=new TextView(this);row.setText(labels[i]);row.setTextSize(15);row.setTypeface(getResources().getFont(R.font.manrope_medium));row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(12),0);row.setFocusable(true);row.setClickable(true);row.setBackgroundResource(R.drawable.input_row_bg);row.setCompoundDrawablePadding(dp(12));
   Drawable icon=getResources().getDrawable(icons[i],getTheme()).mutate();icon.setBounds(0,0,dp(19),dp(19));row.setCompoundDrawables(icon,null,null,null);
   row.setOnFocusChangeListener((v,focus)->{row.setTextColor(focus?0xff121620:0xffedf1fa);icon.setTint(focus?0xff121620:0xffc8d4eb);});row.setTextColor(0xffedf1fa);icon.setTint(0xffc8d4eb);
   row.setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_DPAD_CENTER&&key!=KeyEvent.KEYCODE_ENTER)return false;if(event.getAction()==KeyEvent.ACTION_DOWN){if(openingOk[0]&&event.getRepeatCount()>0)return true;openingOk[0]=false;return false;}if(openingOk[0]){openingOk[0]=false;return true;}return false;});
   row.setOnClickListener(v->{popup.dismiss();if(action==0)savePin(at,!pinned);else if(action==1){try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+app.pkg)));}catch(RuntimeException e){Toast.makeText(this,"App info unavailable",Toast.LENGTH_SHORT).show();}}else launch(at);});
   panel.addView(row,new LinearLayout.LayoutParams(-1,dp(44)));
  }
  // Map the artwork through its complete transform, including the selected tile's enlargement.
  View artwork=((ViewGroup)anchor).getChildAt(0);android.graphics.RectF bounds=new android.graphics.RectF(0,0,artwork.getWidth(),artwork.getHeight());
  android.graphics.Matrix matrix=new android.graphics.Matrix();if(android.os.Build.VERSION.SDK_INT>=29){artwork.transformMatrixToGlobal(matrix);matrix.mapRect(bounds);}else{android.graphics.Rect visible=new android.graphics.Rect();artwork.getGlobalVisibleRect(visible);bounds.set(visible);}
  panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int height=panel.getMeasuredHeight();
  View decor=getWindow().getDecorView();android.graphics.Rect visible=new android.graphics.Rect();decor.getWindowVisibleDisplayFrame(visible);
  int x=Math.max(visible.left+dp(12),Math.min(Math.round(bounds.left),visible.right-width-dp(12)));
  int below=Math.round(bounds.bottom)+dp(10);boolean opensBelow=below+height<=visible.bottom-dp(12);
  int y=opensBelow?below:Math.max(visible.top+dp(12),Math.round(bounds.top)-height-dp(10));
  panel.setAlpha(0f);panel.setScaleX(0.96f);panel.setScaleY(0.96f);panel.setPivotX(Math.max(0,Math.min(width,bounds.centerX()-x)));panel.setPivotY(opensBelow?0:height);panel.setTranslationY(dp(opensBelow?-6:6));
  popup.showAtLocation(decor,Gravity.TOP|Gravity.LEFT,x,y);panel.getChildAt(1).requestFocus();
  panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
 }
 private void launch(int at){try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(apps.get(at).component));}catch(RuntimeException e){Toast.makeText(this,"Could not open app",Toast.LENGTH_SHORT).show();}}
 static final class SelectionRim extends Drawable {
  private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  private final float density;private boolean active;
  private final android.graphics.RectF rim=new android.graphics.RectF();
  SelectionRim(float density){this.density=density;paint.setStyle(android.graphics.Paint.Style.STROKE);}
  @Override public boolean isStateful(){return true;}
  @Override protected boolean onStateChange(int[] states){boolean next=false;for(int state:states)if(state==android.R.attr.state_selected||state==android.R.attr.state_focused||state==android.R.attr.state_pressed)next=true;if(next==active)return false;active=next;invalidateSelf();return true;}
  @Override protected void onBoundsChange(android.graphics.Rect bounds){rim.set(bounds);rim.inset(1.5f*density,1.5f*density);paint.setShader(new android.graphics.LinearGradient(rim.left,rim.top,rim.right,rim.bottom,new int[]{0xff51baff,0xffb681ff,0xff62e5e0},null,android.graphics.Shader.TileMode.CLAMP));paint.setStrokeWidth(3*density);}
  @Override public void draw(android.graphics.Canvas canvas){if(active)canvas.drawRoundRect(rim,12*density,12*density,paint);}
  @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
  @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
  @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
 @Override protected void onDestroy(){
  if(grid!=null){if(navigationFinish!=null)grid.removeCallbacks(navigationFinish);if(navigationSettled!=null)grid.removeCallbacks(navigationSettled);}
  worker.shutdownNow();super.onDestroy();
 }
}
