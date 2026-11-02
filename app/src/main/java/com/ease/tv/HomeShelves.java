package com.ease.tv;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.*;

/** Two independently scrolling shelves, moving together between dock and watching positions. */
final class HomeShelves extends FrameLayout {
 interface Actions {
  void launch(AppAdapter.Entry app);
  void addApps();
  void top();
  void shade(boolean watching,boolean animated);
  void watchFocused(int index);
  void remove(AppAdapter.Entry app);
 }
 private final Actions actions;
 private final LinearLayout page,appsLine,watchLine;
 private final HorizontalScrollView appsScroll,watchScroll;
 private final TextView heading;
 private final List<View> appTiles=new ArrayList<>(),watchTiles=new ArrayList<>();
 private final List<ImageView> watchImages=new ArrayList<>();
 private final Map<View,ShelfProgress> progressBars=new IdentityHashMap<>();
 private final List<HotstarWatchNext.Row> shows=new ArrayList<>();
 private final Map<View,AppAdapter.Entry> appEntries=new IdentityHashMap<>();
 private View moving;
 private boolean moveStepPending;
 private List<View> originalOrder;
 private TextView moveHint;
 private PopupWindow appPopup;
 private String appSignature=null,showIdentity="";
 private int selectedApp,selectedShow;
 private boolean expanded,shadeExpanded;
 private boolean animate;
 private final float density;
 HomeShelves(Context ctx,boolean animate,Actions actions){
  super(ctx);this.actions=actions;this.animate=animate;density=getResources().getDisplayMetrics().density;
  setClipChildren(false);setClipToPadding(false);
  page=new LinearLayout(ctx);page.setOrientation(LinearLayout.VERTICAL);page.setClipChildren(false);page.setClipToPadding(false);
  addView(page,new FrameLayout.LayoutParams(-1,-2));
  appsLine=line();watchLine=line();appsScroll=scroller(appsLine);watchScroll=scroller(watchLine);
  page.addView(appsScroll,new LinearLayout.LayoutParams(-1,dp(154)));
  heading=new TextView(ctx);heading.setText("Continue playing · Hotstar");heading.setTextColor(0xffe5e9f4);heading.setTextSize(17);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(dp(14),0,0,0);
  page.addView(heading,new LinearLayout.LayoutParams(-1,dp(38)));
  page.addView(watchScroll,new LinearLayout.LayoutParams(-1,dp(240)));
  heading.setVisibility(GONE);watchScroll.setVisibility(GONE);
  moveHint=label("← → Move · OK Save · Back Cancel",14);moveHint.setPadding(dp(16),dp(8),dp(16),dp(8));GradientDrawable hintBg=new GradientDrawable();hintBg.setColor(0xee161f32);hintBg.setCornerRadius(dp(12));moveHint.setBackground(hintBg);moveHint.setVisibility(GONE);FrameLayout.LayoutParams hintLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);hintLp.bottomMargin=dp(20);addView(moveHint,hintLp);
  addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)position(false);});
 }
 private int dp(float n){return Math.round(n*density);}
 private LinearLayout line(){LinearLayout v=new LinearLayout(getContext());v.setOrientation(LinearLayout.HORIZONTAL);v.setClipChildren(false);v.setClipToPadding(false);v.setPadding(dp(14),dp(10),dp(14),dp(8));return v;}
 private HorizontalScrollView scroller(LinearLayout line){
  HorizontalScrollView s=new HorizontalScrollView(getContext()){
   private boolean receivingFocus;
   @Override public void requestChildFocus(View child,View focused){receivingFocus=true;try{super.requestChildFocus(child,focused);}finally{receivingFocus=false;}}
   @Override public void scrollTo(int x,int y){if(!receivingFocus)super.scrollTo(x,y);}
   @Override public boolean requestChildRectangleOnScreen(View child,Rect rect,boolean immediate){return false;}
  };
  s.setHorizontalScrollBarEnabled(false);s.setOverScrollMode(OVER_SCROLL_NEVER);s.setClipChildren(false);s.setClipToPadding(false);s.setFocusable(false);s.setSmoothScrollingEnabled(true);s.addView(line,new HorizontalScrollView.LayoutParams(-2,-1));return s;
 }
 private TextView label(String text,int size){TextView t=new TextView(getContext());t.setText(text);t.setTextColor(0xfff2f3fa);t.setTextSize(size);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setGravity(Gravity.CENTER);return t;}
 private LinearLayout tile(int width,int height){
  LinearLayout t=new LinearLayout(getContext());t.setOrientation(LinearLayout.VERTICAL);t.setGravity(Gravity.TOP);t.setFocusable(true);t.setFocusableInTouchMode(true);t.setClipChildren(false);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(height));lp.rightMargin=dp(22);t.setLayoutParams(lp);return t;
 }
 private ImageView artwork(LinearLayout tile,android.graphics.drawable.Drawable image,int height,boolean banner){
  FrameLayout frame=new FrameLayout(getContext());frame.setDuplicateParentStateEnabled(true);frame.setForeground(new AppCatalogActivity.SelectionRim(density));
  ImageView art=new ImageView(getContext());art.setImageDrawable(image);art.setScaleType(banner?ImageView.ScaleType.CENTER_CROP:ImageView.ScaleType.FIT_CENTER);
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff171b24);bg.setCornerRadius(13.5f*density);art.setBackground(bg);art.setClipToOutline(true);art.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
  if(!banner)art.setPadding(dp(24),dp(12),dp(24),dp(12));
  frame.addView(art,new FrameLayout.LayoutParams(-1,-1));tile.addView(frame,new LinearLayout.LayoutParams(-1,dp(height)));return art;
 }
 private LinearLayout appTile(int width,int height){LinearLayout t=tile(width,height);((LinearLayout.LayoutParams)t.getLayoutParams()).rightMargin=dp(30);return t;}
 void setApps(List<AppAdapter.Entry> source){
  List<AppAdapter.Entry> apps=new ArrayList<>(source);
  String saved=getContext().getSharedPreferences("home_shelf",Context.MODE_PRIVATE).getString("order","[]");
  Map<String,Integer> ranks=new HashMap<>();try{org.json.JSONArray order=new org.json.JSONArray(saved);for(int i=0;i<order.length();i++)ranks.put(order.getString(i),i);}catch(org.json.JSONException ignored){}
  apps.sort((a,b)->Integer.compare(ranks.getOrDefault(a.pkg,Integer.MAX_VALUE),ranks.getOrDefault(b.pkg,Integer.MAX_VALUE)));
  int appWidth=Math.max(100,(Math.round(getResources().getDisplayMetrics().widthPixels/density)-80-28-120)/5);
  int appHeight=Math.round(appWidth*9f/16f);
  StringBuilder sig=new StringBuilder();for(AppAdapter.Entry app:apps)sig.append(app.component).append(app.label);
  if(sig.toString().equals(appSignature))return;appSignature=sig.toString();
  org.json.JSONArray canonical=new org.json.JSONArray();for(AppAdapter.Entry app:apps)canonical.put(app.pkg);
  getContext().getSharedPreferences("home_shelf",Context.MODE_PRIVATE).edit().putString("order",canonical.toString()).apply();
  boolean focused=appsLine.hasFocus();if(moving!=null)finishMoving(false);appsLine.removeAllViews();appTiles.clear();appEntries.clear();
  for(AppAdapter.Entry app:apps){
   LinearLayout t=appTile(appWidth,appHeight+34);artwork(t,app.banner!=null?app.banner:app.icon,appHeight,app.banner!=null);t.addView(label(app.label,15),new LinearLayout.LayoutParams(-1,dp(34)));
   final int index=appTiles.size();wire(t,false,index,null);t.setOnClickListener(v->actions.launch(app));t.setOnLongClickListener(v->{showAppMenu(t,app);return true;});appsLine.addView(t);appTiles.add(t);appEntries.put(t,app);
  }
  LinearLayout add=appTile(appWidth,appHeight+34);
  FrameLayout addArt=new FrameLayout(getContext());addArt.setDuplicateParentStateEnabled(true);
  android.graphics.drawable.StateListDrawable backgrounds=new android.graphics.drawable.StateListDrawable();
  GradientDrawable active=new GradientDrawable();active.setColor(0xee44516c);active.setCornerRadius(13.5f*density);
  GradientDrawable resting=new GradientDrawable();resting.setColor(0xcc273247);resting.setCornerRadius(13.5f*density);
  backgrounds.addState(new int[]{android.R.attr.state_selected},active);backgrounds.addState(new int[]{},resting);
  addArt.setBackground(backgrounds);addArt.setForeground(new AppCatalogActivity.SelectionRim(density));
  TextView plus=label("+",36);addArt.addView(plus,new FrameLayout.LayoutParams(-1,-1));add.addView(addArt,new LinearLayout.LayoutParams(-1,dp(appHeight)));
  add.addView(label("Add apps",15),new LinearLayout.LayoutParams(-1,dp(34)));wire(add,false,appTiles.size(),null);add.setOnClickListener(v->actions.addApps());appsLine.addView(add);appTiles.add(add);
  selectedApp=Math.min(selectedApp,appTiles.size()-1);if(focused)focusApps();
 }
 void setShows(List<HotstarWatchNext.Row> rows){
  StringBuilder ids=new StringBuilder();for(HotstarWatchNext.Row r:rows)ids.append(r.id).append('|').append(r.title).append('|').append(r.episode).append('|');
  boolean same=ids.toString().equals(showIdentity);shows.clear();shows.addAll(rows);
  if(same){for(int i=0;i<rows.size();i++){watchImages.get(i).setImageDrawable(HotstarWatchNext.banner(getContext(),rows.get(i),false));ShelfProgress bar=progressBars.get(watchTiles.get(i));bar.row=rows.get(i);bar.invalidate();}return;}
  boolean focused=watchLine.hasFocus();showIdentity=ids.toString();watchLine.removeAllViews();watchTiles.clear();watchImages.clear();progressBars.clear();
  int width=Math.max(220,Math.round((getResources().getDisplayMetrics().widthPixels/density-154)/3));int artHeight=Math.round(width*9f/16f);
  for(int i=0;i<rows.size();i++){
   HotstarWatchNext.Row row=rows.get(i);LinearLayout t=tile(width,artHeight+60);ImageView image=artwork(t,HotstarWatchNext.banner(getContext(),row,false),artHeight,true);watchImages.add(image);
   FrameLayout art=(FrameLayout)image.getParent();GradientDrawable clip=new GradientDrawable();clip.setColor(0xff171b24);clip.setCornerRadius(13.5f*density);art.setBackground(clip);art.setOutlineProvider(ViewOutlineProvider.BACKGROUND);art.setClipToOutline(true);
   boolean hasEpisode=row.episode!=null&&!row.episode.trim().isEmpty();
   View episodeShade=new View(getContext());episodeShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0xd9000000}));episodeShade.setAlpha(0f);FrameLayout.LayoutParams shadeLp=new FrameLayout.LayoutParams(-1,dp(78),Gravity.BOTTOM);art.addView(episodeShade,shadeLp);
   TextView episode=label(hasEpisode?row.episode:"",12);episode.setSingleLine(false);episode.setMaxLines(2);episode.setGravity(Gravity.LEFT|Gravity.BOTTOM);episode.setTextColor(0xfff1f4fb);episode.setTypeface(getResources().getFont(R.font.manrope_medium));episode.setAlpha(0f);episode.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
   FrameLayout.LayoutParams episodeLp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);episodeLp.leftMargin=dp(13);episodeLp.rightMargin=dp(13);episodeLp.bottomMargin=dp(24);art.addView(episode,episodeLp);
   ShelfProgress progress=new ShelfProgress(getContext(),row,episode,episodeShade,hasEpisode);art.addView(progress,new FrameLayout.LayoutParams(-1,-1));progressBars.put(t,progress);
   LinearLayout caption=new LinearLayout(getContext());caption.setOrientation(LinearLayout.VERTICAL);caption.addView(label(row.title,16),new LinearLayout.LayoutParams(-1,dp(28)));caption.setAlpha(0f);t.addView(caption);
   final int index=i;wire(t,true,i,caption);t.setOnClickListener(v->{if(index<shows.size())HotstarWatchNext.open(getContext(),shows.get(index));});t.setOnLongClickListener(v->true);watchLine.addView(t);watchTiles.add(t);
  }
  selectedShow=Math.min(selectedShow,Math.max(0,rows.size()-1));heading.setVisibility(rows.isEmpty()?GONE:VISIBLE);watchScroll.setVisibility(rows.isEmpty()?GONE:VISIBLE);
  if(rows.isEmpty()){expanded=false;if(focused)focusApps();}else if(focused)focusShows();position(false);
 }
 private void wire(View tile,boolean watch,int index,View caption){
  tile.setOnFocusChangeListener((v,focus)->{
   v.setSelected(focus);if(v instanceof ViewGroup)((ViewGroup)v).getChildAt(0).setSelected(focus);v.animate().scaleX(focus?(watch?1.05f:1.2f):1f).scaleY(focus?(watch?1.05f:1.2f):1f).setDuration(animate?160:0).setInterpolator(new DecelerateInterpolator()).start();
   ShelfProgress progress=progressBars.get(v);if(progress!=null)progress.highlight(focus);
   if(caption!=null)caption.animate().alpha(focus?1f:0f).setDuration(animate?120:0).start();
   if(focus){if(watch){selectedShow=index;expanded=true;actions.watchFocused(index);}else{selectedApp=appTiles.indexOf(v);expanded=false;}position(animate);reveal(watch?watchScroll:appsScroll,v);}
  });
  tile.setOnKeyListener((v,key,event)->{
   if(key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN&&key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;
   if(event.getAction()!=KeyEvent.ACTION_DOWN)return true;
   if(key==KeyEvent.KEYCODE_DPAD_UP){if(watch)focusApps();else actions.top();}
   else if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(!watch)focusShows();}
   else{List<View> tiles=watch?watchTiles:appTiles;int next=(watch?index:tiles.indexOf(v))+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);if(next>=0&&next<tiles.size())tiles.get(next).requestFocus();}
   return true;
  });
 }
 private void showAppMenu(View tile,AppAdapter.Entry app){
  LinearLayout panel=new LinearLayout(getContext());panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(6),dp(6),dp(6),dp(6));GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xff292e3a,0xff171c27});bg.setCornerRadius(dp(18));bg.setStroke(dp(1),0xff47526a);panel.setBackground(bg);
  PopupWindow popup=new PopupWindow(panel,dp(230),-2,true);appPopup=popup;popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));popup.setOutsideTouchable(true);popup.setElevation(dp(16));popup.setAnimationStyle(0);
  TextView title=label(app.label,12);title.setGravity(Gravity.LEFT);title.setTextColor(0xffaabbd7);title.setPadding(dp(12),dp(6),dp(12),dp(8));panel.addView(title);
  final boolean[] opening={true};String[] labels={"Move","Remove from Home"};
  for(int i=0;i<2;i++){final int action=i;TextView row=label(labels[i],15);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(12),0);row.setFocusable(true);row.setClickable(true);row.setBackgroundResource(R.drawable.input_row_bg);android.graphics.drawable.Drawable icon=getResources().getDrawable(action==0?R.drawable.ic_move_horizontal:R.drawable.ic_apps_pill,getContext().getTheme()).mutate();icon.setBounds(0,0,dp(18),dp(18));icon.setTint(0xffc8d4eb);row.setCompoundDrawables(icon,null,null,null);row.setCompoundDrawablePadding(dp(12));row.setOnFocusChangeListener((v,f)->{row.setTextColor(f?0xff121722:0xffeef3fc);icon.setTint(f?0xff121722:0xffc8d4eb);});row.setOnKeyListener((v,k,e)->{if(k!=KeyEvent.KEYCODE_DPAD_CENTER&&k!=KeyEvent.KEYCODE_ENTER)return false;if(e.getAction()==KeyEvent.ACTION_DOWN){if(opening[0]&&e.getRepeatCount()>0)return true;opening[0]=false;return false;}if(opening[0]){opening[0]=false;return true;}return false;});row.setOnClickListener(v->{popup.dismiss();if(action==0)beginMoving(tile);else actions.remove(app);});panel.addView(row,new LinearLayout.LayoutParams(-1,dp(44)));}
  View art=((android.view.ViewGroup)tile).getChildAt(0);android.graphics.RectF bounds=new android.graphics.RectF(0,0,art.getWidth(),art.getHeight());android.graphics.Matrix matrix=new android.graphics.Matrix();if(android.os.Build.VERSION.SDK_INT>=29){art.transformMatrixToGlobal(matrix);matrix.mapRect(bounds);}else{android.graphics.Rect visible=new android.graphics.Rect();art.getGlobalVisibleRect(visible);bounds.set(visible);}
  panel.measure(MeasureSpec.makeMeasureSpec(dp(230),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int h=panel.getMeasuredHeight();android.graphics.Rect visible=new android.graphics.Rect();getWindowVisibleDisplayFrame(visible);int x=Math.max(dp(12),Math.min(Math.round(bounds.left),visible.right-dp(242)));boolean below=bounds.bottom+dp(10)+h<visible.bottom-dp(12);int y=below?Math.round(bounds.bottom)+dp(10):Math.max(visible.top+dp(12),Math.round(bounds.top)-h-dp(10));panel.setAlpha(0);panel.setScaleY(0.95f);panel.setPivotY(below?0:h);popup.showAtLocation(this,Gravity.TOP|Gravity.LEFT,x,y);panel.getChildAt(1).requestFocus();panel.animate().alpha(1).scaleY(1).setDuration(180).start();
 }
 private void beginMoving(View tile){originalOrder=new ArrayList<>(appTiles);moving=tile;moveHint.setVisibility(VISIBLE);TextView name=(TextView)((android.view.ViewGroup)tile).getChildAt(1);name.setText("‹  "+appEntries.get(tile).label+"  ›");tile.requestFocus();}
 boolean handleMoveKey(KeyEvent event){
  if(moving==null)return false;int key=event.getKeyCode();boolean down=event.getAction()==KeyEvent.ACTION_DOWN;
  if(key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_ESCAPE){if(!down)finishMoving(false);return true;}
  if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(!down)finishMoving(true);return true;}
  if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){
   if(down&&!moveStepPending){int from=appTiles.indexOf(moving),to=from+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);if(to>=0&&to<appTiles.size()-1)slideSwap(from,to);}
   return true;
  }
  return key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN;
 }
 private void slideSwap(int from,int to){
  View carried=moving,neighbour=appTiles.get(to);
  float carriedX=carried.getLeft()+carried.getTranslationX();
  float neighbourX=neighbour.getLeft()+neighbour.getTranslationX();
  moveStepPending=true;
  Collections.swap(appTiles,from,to);appsLine.removeView(carried);appsLine.addView(carried,to);carried.requestFocus();selectedApp=to;
  appsLine.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
   public boolean onPreDraw(){
    appsLine.getViewTreeObserver().removeOnPreDrawListener(this);moveStepPending=false;
    if(moving!=carried)return true;
    if(animate){
     carried.setTranslationX(carriedX-carried.getLeft());neighbour.setTranslationX(neighbourX-neighbour.getLeft());
     carried.animate().translationX(0).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
     neighbour.animate().translationX(0).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
    }
    reveal(appsScroll,carried);return true;
   }
  });
 }
 private void finishMoving(boolean save){
  if(moving==null)return;View carried=moving;
  if(save){org.json.JSONArray order=new org.json.JSONArray();for(View tile:appTiles){AppAdapter.Entry app=appEntries.get(tile);if(app!=null)order.put(app.pkg);}if(!getContext().getSharedPreferences("home_shelf",Context.MODE_PRIVATE).edit().putString("order",order.toString()).commit()){Toast.makeText(getContext(),"Could not save order",Toast.LENGTH_SHORT).show();save=false;}}
  moving=null;moveStepPending=false;for(View tile:appTiles){tile.animate().cancel();tile.setTranslationX(0);}if(!save){appTiles.clear();appTiles.addAll(originalOrder);appsLine.removeAllViews();for(View tile:appTiles)appsLine.addView(tile);}originalOrder=null;
  ((TextView)((android.view.ViewGroup)carried).getChildAt(1)).setText(appEntries.get(carried).label);moveHint.setVisibility(GONE);carried.requestFocus();selectedApp=appTiles.indexOf(carried);reveal(appsScroll,carried);
 }
 void cancelMoving(){if(appPopup!=null)appPopup.dismiss();finishMoving(false);}
 private final class ShelfProgress extends View {
  private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
  private final TextView episode;private final View episodeShade;private final boolean hasEpisode;
  HotstarWatchNext.Row row;private float lift;private android.animation.ValueAnimator motion;
  ShelfProgress(Context context,HotstarWatchNext.Row row,TextView episode,View shade,boolean hasEpisode){super(context);this.row=row;this.episode=episode;this.episodeShade=shade;this.hasEpisode=hasEpisode;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
  void highlight(boolean selected){
   if(motion!=null)motion.cancel();
   if(!animate){lift=selected?1f:0f;updateEpisode();invalidate();return;}
   motion=android.animation.ValueAnimator.ofFloat(lift,selected?1f:0f);motion.setDuration(220);motion.setInterpolator(new DecelerateInterpolator());motion.addUpdateListener(a->{lift=(Float)a.getAnimatedValue();updateEpisode();invalidate();});motion.start();
  }
  private void updateEpisode(){episode.setAlpha(hasEpisode?lift:0f);episodeShade.setAlpha(hasEpisode?lift:0f);episode.setTranslationY(dp(6)*(1-lift));}
  @Override protected void onDraw(android.graphics.Canvas canvas){
   float inset=dp(13)*lift,bottom=getHeight()-dp(12)*lift,height=dp(3),radius=height*0.5f*lift;
   android.graphics.RectF track=new android.graphics.RectF(inset,bottom-height,getWidth()-inset,bottom);
   paint.setColor(0xff65708b);canvas.drawRoundRect(track,radius,radius,paint);
   float fraction=row.duration>0?Math.max(0f,Math.min(1f,(float)row.position/row.duration)):0f;
   if(fraction>0){canvas.save();android.graphics.Path shape=new android.graphics.Path();shape.addRoundRect(track,radius,radius,android.graphics.Path.Direction.CW);canvas.clipPath(shape);track.right=track.left+track.width()*fraction;paint.setColor(0xff86b6ff);canvas.drawRoundRect(track,radius,radius,paint);canvas.restore();}
  }
 }
 private void reveal(HorizontalScrollView scroll,View tile){scroll.post(()->{int left=tile.getLeft()-dp(12),right=tile.getRight()+dp(12);int x=scroll.getScrollX();if(left<x)x=left;else if(right>x+scroll.getWidth())x=right-scroll.getWidth();x=Math.max(0,x);if(animate)scroll.smoothScrollTo(x,0);else scroll.scrollTo(x,0);});}
 private void position(boolean animated){
  if(shadeExpanded!=expanded){shadeExpanded=expanded;actions.shade(expanded,animated);}
  float y=expanded?dp(8):Math.max(dp(8),getHeight()-dp(154)-(shows.isEmpty()?dp(30):dp(66)));
  page.animate().cancel();if(animated)page.animate().translationY(y).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();else page.setTranslationY(y);
 }
 void setMotion(boolean enabled){animate=enabled;if(!enabled){page.animate().cancel();position(false);}}
 void focusApps(){if(!appTiles.isEmpty())appTiles.get(Math.min(selectedApp,appTiles.size()-1)).requestFocus();}
 private void focusShows(){if(!watchTiles.isEmpty())watchTiles.get(Math.min(selectedShow,watchTiles.size()-1)).requestFocus();}
 int posterStart(){return Math.max(0,selectedShow-1);}
 boolean backToApps(){if(expanded){focusApps();return true;}return false;}
}
