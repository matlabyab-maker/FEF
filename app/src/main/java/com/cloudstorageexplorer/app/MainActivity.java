package com.cloudstorageexplorer.android;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list; TextView path;
    static final int PICK_TREE=41, SEND=42;
    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.DKGRAY); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(14),dp(8),dp(14),dp(8)); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setMinHeight(dp(52)); return b; }
    @Override public void onCreate(Bundle b){ super.onCreate(b); build(); }
    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=tv("Cloud Explorer",20); title.setTypeface(null,1); bar.addView(title,new LinearLayout.LayoutParams(0,dp(62),1));
        Button menu=btn("☰"); bar.addView(menu,new LinearLayout.LayoutParams(dp(64),dp(62))); root.addView(bar);
        path=tv("Internal Storage",16); path.setBackgroundColor(Color.rgb(245,245,245)); root.addView(path,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout actions=new LinearLayout(this); actions.setPadding(dp(6),0,dp(6),0);
        Button add=btn("☁ Add Cloud"); add.setOnClickListener(v->addCloud()); actions.addView(add,new LinearLayout.LayoutParams(0,dp(58),1));
        Button share=btn("↗ Share"); share.setOnClickListener(v->shareFile()); actions.addView(share,new LinearLayout.LayoutParams(0,dp(58),1)); root.addView(actions);
        TextView info=tv("Cloud storage uses Android's standard provider system. Google Drive, Dropbox, OneDrive, Box and other installed providers can appear here when available.",14); info.setPadding(dp(14),dp(10),dp(14),dp(10)); root.addView(info);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); ScrollView sv=new ScrollView(this); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        addStorage("📱  Internal Storage","/storage/emulated/0"); addStorage("💾  SD Card","Removable storage (if available)"); addStorage("🔌  USB / External HDD","OTG storage (when connected)");
        TextView cloud=tv("☁  Cloud providers",18); cloud.setTypeface(null,1); cloud.setPadding(dp(14),dp(18),dp(14),dp(8)); list.addView(cloud);
        addProvider("Google Drive"); addProvider("Dropbox"); addProvider("OneDrive"); addProvider("Box"); addProvider("Other Android cloud providers");
        setContentView(root);
    }
    void addStorage(String name,String sub){ LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(6),dp(2),dp(6),dp(2)); TextView a=tv(name,17); TextView c=tv(sub,13); c.setTextColor(Color.GRAY); row.addView(a); row.addView(c); row.setOnClickListener(v->openTree()); list.addView(row,new LinearLayout.LayoutParams(-1,dp(72))); }
    void addProvider(String name){ Button b=btn("☁  "+name); b.setOnClickListener(v->addCloud()); list.addView(b,new LinearLayout.LayoutParams(-1,dp(60))); }
    void addCloud(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); startActivityForResult(i,PICK_TREE); }
    void openTree(){ addCloud(); }
    void shareFile(){ Intent i=new Intent(Intent.ACTION_SEND); i.setType("*/*"); i.putExtra(Intent.EXTRA_TEXT,"Select a file in Fast File Manager, then use Share to send it to Google Drive, Dropbox, OneDrive, Box or another installed provider."); startActivity(Intent.createChooser(i,"Share to cloud or app")); }
    @Override protected void onActivityResult(int r,int c,Intent d){ super.onActivityResult(r,c,d); if(r==PICK_TREE && c==RESULT_OK && d!=null){ Uri u=d.getData(); try{ getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }catch(Exception ignored){} path.setText("Cloud added: "+u.toString()); Toast.makeText(this,"Cloud storage added",Toast.LENGTH_SHORT).show(); } }
}
