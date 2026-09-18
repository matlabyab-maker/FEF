package com.cloudstorageexplorer.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_TREE = 100;
    private LinearLayout list, root;
    private TextView path;
    private DocumentFile currentDir;
    private final ArrayDeque<DocumentFile> history = new ArrayDeque<>();
    private final ArrayList<Uri> savedRoots = new ArrayList<>();
    private android.content.SharedPreferences prefs;

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    TextView text(String s,float size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.DKGRAY); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(14),dp(7),dp(14),dp(7)); return t; }
    Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(14); b.setAllCaps(false); b.setMinHeight(dp(52)); return b; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("roots",MODE_PRIVATE); loadRoots(); buildHome(); }

    void buildHome(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Cloud Storage Explorer",20); title.setTypeface(null,1); top.addView(title,new LinearLayout.LayoutParams(0,dp(60),1));
        Button add=button("☁ Add Cloud"); add.setOnClickListener(v->pickRoot()); top.addView(add,new LinearLayout.LayoutParams(dp(125),dp(60)));
        root.addView(top);
        path=text("Storage locations",16); path.setBackgroundColor(Color.rgb(245,245,245)); root.addView(path,new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout bar=new LinearLayout(this);
        Button device=button("📱 Open Storage"); device.setOnClickListener(v->pickRoot()); bar.addView(device,new LinearLayout.LayoutParams(0,dp(56),1));
        Button back=button("← Back"); back.setOnClickListener(v->goBack()); bar.addView(back,new LinearLayout.LayoutParams(0,dp(56),1));
        root.addView(bar);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); ScrollView sv=new ScrollView(this); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); showRoots();
    }

    void showRoots(){
        list.removeAllViews();
        addSection("📱 Device and removable storage");
        TextView hint=text("Android opens the storage/provider selector. Choose Internal Storage, SD Card or a USB/OTG drive. The selected location is remembered.",14); hint.setTextColor(Color.GRAY); list.addView(hint,new LinearLayout.LayoutParams(-1,dp(68)));
        Button open=button("📂 Select a storage location"); open.setOnClickListener(v->pickRoot()); list.addView(open,new LinearLayout.LayoutParams(-1,dp(62)));
        if(!savedRoots.isEmpty()) addSection("☁ Saved cloud / storage locations");
        for(Uri u:savedRoots){ DocumentFile d=DocumentFile.fromTreeUri(this,u); String name=d!=null&&d.getName()!=null?d.getName():u.toString(); Button r=button("☁  "+name); r.setOnClickListener(v->openRoot(u)); list.addView(r,new LinearLayout.LayoutParams(-1,dp(62))); }
    }

    void addSection(String s){ TextView h=text(s,18); h.setTypeface(null,1); h.setPadding(dp(14),dp(16),dp(14),dp(8)); list.addView(h,new LinearLayout.LayoutParams(-1,dp(52))); }

    void pickRoot(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); startActivityForResult(i,PICK_TREE); }

    void openRoot(Uri u){ history.clear(); DocumentFile d=DocumentFile.fromTreeUri(this,u); if(d!=null) browse(d); }

    void browse(DocumentFile dir){
        if(dir==null){ Toast.makeText(this,"Storage is not available",Toast.LENGTH_SHORT).show(); return; }
        currentDir = dir;
        String dirName = dir.getName();
        path.setText(dirName != null ? dirName : dir.getUri().toString());
        list.removeAllViews();
        Button home=button("⌂ Storage locations"); home.setOnClickListener(v->buildHome()); list.addView(home,new LinearLayout.LayoutParams(-1,dp(54)));
        DocumentFile[] files=dir.listFiles(); Arrays.sort(files,(a,b)->{ boolean ad=a.isDirectory(), bd=b.isDirectory(); if(ad!=bd)return ad?-1:1; return a.getName()==null?"".compareTo(b.getName()==null?"":b.getName()):a.getName().compareToIgnoreCase(b.getName()); });
        if(files.length==0){ TextView empty=text("This folder is empty",16); list.addView(empty,new LinearLayout.LayoutParams(-1,dp(60))); return; }
        for(DocumentFile f:files) addFileRow(f);
    }

    void addFileRow(DocumentFile f){
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        String icon=f.isDirectory()?"📁":"📄"; TextView name=text(icon+"  "+(f.getName()==null?"Unnamed":f.getName()),16); row.addView(name,new LinearLayout.LayoutParams(0,dp(64),1));
        if(!f.isDirectory()){ Button sh=button("↗"); sh.setMinWidth(dp(54)); sh.setOnClickListener(v->share(f)); row.addView(sh,new LinearLayout.LayoutParams(dp(60),dp(60))); }
        row.setOnClickListener(v->{ if(f.isDirectory()){ if(currentDir!=null) history.push(currentDir); browse(f); } else { share(f); } });
        list.addView(row,new LinearLayout.LayoutParams(-1,dp(68)));
    }

    void share(DocumentFile f){
        Intent i=new Intent(Intent.ACTION_SEND); i.setType(f.getType()!=null?f.getType():"application/octet-stream"); i.putExtra(Intent.EXTRA_STREAM,f.getUri()); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(Intent.createChooser(i,"Share file"));
    }

    void goBack(){ if(!history.isEmpty()) browse(history.pop()); else buildHome(); }

    void loadRoots(){ String all=prefs.getString("uris",""); if(!all.isEmpty()) for(String s:all.split("\\n")){ try{ savedRoots.add(Uri.parse(s)); }catch(Exception ignored){} } }
    void saveRoot(Uri u){ StringBuilder sb=new StringBuilder(); for(Uri x:savedRoots){ if(!x.equals(u)) sb.append(x).append('\n'); } sb.append(u); prefs.edit().putString("uris",sb.toString()).apply(); }

    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_TREE && c==RESULT_OK && d!=null && d.getData()!=null){ Uri u=d.getData(); try{ getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }catch(Exception ignored){} if(!savedRoots.contains(u)){savedRoots.add(u); saveRoot(u);} openRoot(u); } }
}
