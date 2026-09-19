package com.cloudexplorer.finalapp;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    static final int PICK_TREE=10, REQ_STORAGE=20;
    static final String[] LEGACY_PERMS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    LinearLayout root,list; TextView path,status;
    File currentDir; Uri currentTree;
    final ArrayList<File> items=new ArrayList<>();
    final ArrayList<File> selected=new ArrayList<>();
    final ArrayList<Uri> cloudRoots=new ArrayList<>();
    SharedPreferences prefs;
    String sort="name"; boolean foldersFirst=true; boolean descending=false;

    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("state",0); buildUI(); loadClouds(); showHome(); requestStorageAccess();}

    TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.DKGRAY);t.setPadding(18,14,18,14);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setMinHeight(56);return b;}

    void buildUI(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); setContentView(root);
        LinearLayout bar=new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
        String[] bs={"☰","←","⟳","Select","Copy","Cut","Paste","Delete","Rename","Share","ZIP","Properties","Search","Cloud +","Sort","Favorites"};
        for(String s:bs){Button b=btn(s);bar.addView(b,new LinearLayout.LayoutParams(-2,-2));b.setOnClickListener(v->action(s));}
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.addView(bar);root.addView(hsv,new LinearLayout.LayoutParams(-1,-2));
        path=tv("Storage & Cloud",18);root.addView(path);
        status=tv("Checking storage permission…",14);root.addView(status);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv=new ScrollView(this);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    }

    void requestStorageAccess(){
        if(Build.VERSION.SDK_INT>=30){
            if(Environment.isExternalStorageManager()){
                status.setText("Storage access: allowed");
                return;
            }
            new AlertDialog.Builder(this)
                .setTitle("Storage permission")
                .setMessage("برای نمایش و مدیریت فایل‌های حافظه داخلی و حافظه‌های قابل دسترس، اجازه دسترسی به فایل‌ها را فعال کنید.")
                .setPositiveButton("Allow",(d,w)->{
                    try {
                        Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        i.setData(Uri.parse("package:"+getPackageName()));
                        startActivity(i);
                    } catch(Exception e){
                        try { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); } catch(Exception ignored) {}
                    }
                })
                .setNegativeButton("Cancel",null).show();
        } else if(Build.VERSION.SDK_INT>=23){
            boolean read=ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
            boolean write=ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
            if(read && write){
                status.setText("Storage access: allowed");
            } else {
                status.setText("Storage permission required");
                requestPermissions(LEGACY_PERMS,REQ_STORAGE);
            }
        } else {
            status.setText("Storage access: allowed");
        }
    }

    @Override protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT>=30){
            if(Environment.isExternalStorageManager()){
                status.setText("Storage access: allowed");
                if(currentDir==null) showHome();
            } else {
                status.setText("Storage permission required");
            }
        } else if(Build.VERSION.SDK_INT>=23){
            boolean ok=ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
            status.setText(ok?"Storage access: allowed":"Storage permission required");
            if(ok && currentDir==null) showHome();
        }
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_STORAGE){if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){status.setText("Storage access: allowed");showHome();}else status.setText("Storage permission denied");}}

    void showHome(){
        currentDir=null; currentTree=null; selected.clear(); list.removeAllViews(); path.setText("Storage & Cloud");
        boolean allowed = Build.VERSION.SDK_INT < 23 || (Build.VERSION.SDK_INT>=30 ? Environment.isExternalStorageManager() : ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);
        File internal=Environment.getExternalStorageDirectory();
        if(allowed && internal!=null){
            addRow("📱 Internal Storage  •  "+human(internal.getFreeSpace())+" free / "+human(internal.getTotalSpace()),()->openLocal(internal));
        } else {
            addRow("🔒 Internal Storage  •  Allow access first",this::requestStorageAccess);
        }
        File storage=new File("/storage");
        File[] roots=storage.listFiles();
        if(allowed && roots!=null){
            for(File f:roots){
                String p=f.getAbsolutePath();
                if(f.isDirectory() && !p.equals("/storage/emulated") && !p.equals(internal==null?"":internal.getAbsolutePath())){
                    addRow("💾 "+f.getName()+"  •  "+human(f.getFreeSpace())+" free / "+human(f.getTotalSpace()),()->openLocal(f));
                }
            }
        }
        addRow("🔐 Storage Permission",this::requestStorageAccess);
        for(Uri u:cloudRoots){
            DocumentFile d=DocumentFile.fromTreeUri(this,u);
            if(d!=null) addRow("☁️ "+(d.getName()==null?"Cloud Storage":d.getName()),()->openCloud(u));
        }
        addRow("☁️ Add Cloud / Storage",this::pickTree);
        addRow("⭐ Favorites",this::favorites);
    }

    void openLocal(File f){if(f==null){toast("Storage unavailable");return;}currentDir=f;currentTree=null;refreshLocal();}
    void refreshLocal(){list.removeAllViews();if(currentDir==null){showHome();return;}path.setText(currentDir.getAbsolutePath());File[] a=currentDir.listFiles();items.clear();if(a!=null)items.addAll(Arrays.asList(a));sortItems();for(File f:items)addFileRow(f);status.setText(items.size()+" items • "+human(currentDir.getFreeSpace())+" free / "+human(currentDir.getTotalSpace()));}

    void openCloud(Uri u){
        currentTree=u; currentDir=null; list.removeAllViews();
        DocumentFile d=DocumentFile.fromTreeUri(this,u);
        if(d==null) d=DocumentFile.fromSingleUri(this,u);
        if(d==null || !d.isDirectory()){toast("Cloud folder unavailable"); return;}
        path.setText("☁️ "+(d.getName()==null?u.toString():d.getName()));
        DocumentFile[] a=d.listFiles(); sortDocs(a); for(DocumentFile f:a) addDocRow(f);
        status.setText(a.length+" items • Cloud/SAF");
    }

    void sortDocs(DocumentFile[] a){Arrays.sort(a,(x,y)->{if(foldersFirst&&x.isDirectory()!=y.isDirectory())return x.isDirectory()?-1:1;String xn=x.getName()==null?"":x.getName();String yn=y.getName()==null?"":y.getName();int c=xn.compareToIgnoreCase(yn);return descending?-c:c;});}
    void addDocRow(DocumentFile f){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView n=tv((f.isDirectory()?"📁 ":icon(f.getName())+" ")+(f.getName()==null?"Unnamed":f.getName()),17);
        r.addView(n,new LinearLayout.LayoutParams(0,-2,1));
        TextView z=tv(f.isDirectory()?"Folder":human(f.length()),13); r.addView(z);
        r.setOnClickListener(v->{
            if(f.isDirectory()){ currentTree=f.getUri(); openCloud(f.getUri()); } else { toast("Cloud file: "+f.getName()); }
        });
        r.setOnLongClickListener(v->{toast("Cloud item: "+f.getName());return true;}); list.addView(r);
    }


    void sortItems(){Collections.sort(items,(a,b)->{if(foldersFirst&&a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;int c;if(sort.equals("size"))c=Long.compare(a.length(),b.length());else if(sort.equals("date"))c=Long.compare(a.lastModified(),b.lastModified());else if(sort.equals("type"))c=ext(a).compareToIgnoreCase(ext(b));else c=a.getName().compareToIgnoreCase(b.getName());return descending?-c:c;});}
    void addFileRow(File f){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);TextView n=tv((f.isDirectory()?"📁 ":icon(f)+" ")+f.getName(),17);r.addView(n,new LinearLayout.LayoutParams(0,-2,1));TextView z=tv(f.isDirectory()?"Folder":human(f.length()),13);r.addView(z);r.setOnClickListener(v->{if(f.isDirectory())openLocal(f);else selectOne(f);});r.setOnLongClickListener(v->{selectOne(f);return true;});list.addView(r);}
    void selectOne(File f){selected.clear();selected.add(f);status.setText("Selected: "+f.getName());}

    void action(String s){try{switch(s){case"☰":showHome();break;case"←":goBack();break;case"⟳":if(currentTree!=null)openCloud(currentTree);else refreshLocal();break;case"Select":selectMode();break;case"Copy":copy(false);break;case"Cut":copy(true);break;case"Paste":paste();break;case"Delete":deleteSelected();break;case"Rename":rename();break;case"Share":share();break;case"ZIP":zipSelected();break;case"Properties":properties();break;case"Search":search();break;case"Cloud +":pickTree();break;case"Sort":chooseSort();break;case"Favorites":favorites();break;}}catch(Exception e){toast(e.getMessage());}}
    void goBack(){if(currentDir!=null){File p=currentDir.getParentFile();if(p!=null&&p.getAbsolutePath().startsWith("/storage")){openLocal(p);return;}showHome();}else if(currentTree!=null){showHome();}else showHome();}

    void selectMode(){selected.clear();list.removeAllViews();if(currentDir!=null)for(File f:items){CheckBox c=new CheckBox(this);c.setText((f.isDirectory()?"📁 ":icon(f)+" ")+f.getName());c.setTextSize(17);c.setPadding(12,12,12,12);c.setOnCheckedChangeListener((b,x)->{if(x)selected.add(f);else selected.remove(f);});list.addView(c);}status.setText("Select files, then use an action");}
    void copy(boolean cut){if(selected.isEmpty()){toast("Select files first");return;}prefs.edit().putString("clipboard",join(selected)).putBoolean("cut",cut).apply();status.setText((cut?"Cut ":"Copied ")+selected.size()+" item(s)");}
    void paste(){String p=prefs.getString("clipboard","");if(p.isEmpty()||currentDir==null){toast("Paste is available for local storage");return;}for(String q:p.split("\\n"))if(!q.isEmpty())try{File src=new File(q),dst=new File(currentDir,src.getName());copyRec(src,dst);}catch(Exception e){toast(e.getMessage());}if(prefs.getBoolean("cut",false))prefs.edit().remove("clipboard").apply();refreshLocal();}
    void copyRec(File s,File d)throws IOException{if(s.isDirectory()){if(!d.exists()&&!d.mkdirs())throw new IOException("Cannot create folder");File[] x=s.listFiles();if(x!=null)for(File f:x)copyRec(f,new File(d,f.getName()));}else{try(InputStream in=new FileInputStream(s);OutputStream out=new FileOutputStream(d)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)out.write(b,0,n);}}}
    void deleteSelected(){if(selected.isEmpty())return;new AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete "+selected.size()+" item(s)?").setPositiveButton("Delete",(d,w)->{for(File f:selected)del(f);selected.clear();refreshLocal();}).setNegativeButton("Cancel",null).show();}
    boolean del(File f){if(f.isDirectory()){File[] x=f.listFiles();if(x!=null)for(File c:x)del(c);}return f.delete();}
    void rename(){if(selected.size()!=1){toast("Select one local item");return;}File f=selected.get(0);EditText e=new EditText(this);e.setText(f.getName());new AlertDialog.Builder(this).setTitle("Rename").setView(e).setPositiveButton("Rename",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())f.renameTo(new File(f.getParentFile(),n));refreshLocal();}).setNegativeButton("Cancel",null).show();}
    void share(){if(selected.isEmpty()){toast("Select files first");return;}Intent i=new Intent(Intent.ACTION_SEND_MULTIPLE);i.setType("*/*");ArrayList<Uri> us=new ArrayList<>();for(File f:selected)try{File c=new File(getCacheDir(),f.getName());copyRec(f,c);us.add(FileProvider.getUriForFile(this,getPackageName()+".files",c));}catch(Exception ignored){}i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.putParcelableArrayListExtra(Intent.EXTRA_STREAM,us);try{startActivity(Intent.createChooser(i,"Share files"));}catch(Exception e){toast("No share app available");}}
    void zipSelected(){if(selected.isEmpty()||currentDir==null){toast("ZIP requires local files");return;}File out=new File(currentDir,"Archive_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".zip");try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out))){for(File f:selected)zipAdd(z,f,f.getName());toast("ZIP created");refreshLocal();}catch(Exception e){toast(e.getMessage());}}
    void zipAdd(ZipOutputStream z,File f,String name)throws IOException{if(f.isDirectory()){z.putNextEntry(new ZipEntry(name+"/"));z.closeEntry();File[] x=f.listFiles();if(x!=null)for(File c:x)zipAdd(z,c,name+"/"+c.getName());}else{z.putNextEntry(new ZipEntry(name));try(FileInputStream in=new FileInputStream(f)){byte[] b=new byte[16384];int n;while((n=in.read(b))>0)z.write(b,0,n);}z.closeEntry();}}
    void properties(){if(selected.size()!=1){toast("Select one item");return;}File f=selected.get(0);String s="Name: "+f.getName()+"\nType: "+ext(f)+"\nSize: "+human(f.length())+"\nPath: "+f.getAbsolutePath()+"\nModified: "+new Date(f.lastModified());new AlertDialog.Builder(this).setTitle("Properties").setMessage(s).setPositiveButton("OK",null).setNeutralButton("Copy text",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Properties",s));}).show();}
    void search(){if(currentDir==null)return;EditText e=new EditText(this);e.setHint("File or folder name");new AlertDialog.Builder(this).setTitle("Search current folder").setView(e).setPositiveButton("Search",(d,w)->{String q=e.getText().toString().toLowerCase(Locale.ROOT);list.removeAllViews();for(File f:items)if(f.getName().toLowerCase(Locale.ROOT).contains(q))addFileRow(f);}).setNegativeButton("Cancel",null).show();}
    void chooseSort(){String[] x={"Name","Type / Extension","Size","Modified date","Reverse order"};new AlertDialog.Builder(this).setTitle("Sort").setItems(x,(d,w)->{if(w==4)descending=!descending;else sort=w==0?"name":w==1?"type":w==2?"size":"date";if(currentDir!=null)refreshLocal();});}
    void favorites(){list.removeAllViews();path.setText("Favorites");String raw=prefs.getString("favorites","");for(String p:raw.split("\\n"))if(!p.isEmpty()){File f=new File(p);if(f.exists())addRow("⭐ "+f.getName(),()->selectOne(f));}}
    void addRow(String s,final Runnable r){TextView t=tv(s,18);t.setOnClickListener(v->r.run());list.addView(t);}

    void pickTree(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,PICK_TREE);}
    void loadClouds(){for(String s:prefs.getStringSet("clouds",new LinkedHashSet<String>()))try{cloudRoots.add(Uri.parse(s));}catch(Exception ignored){}}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;if(r==PICK_TREE){Uri u=d.getData();try{getContentResolver().takePersistableUriPermission(u,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}cloudRoots.add(u);LinkedHashSet<String> s=new LinkedHashSet<>();for(Uri x:cloudRoots)s.add(x.toString());prefs.edit().putStringSet("clouds",s).apply();toast("Storage / Cloud added");showHome();}}

    String join(ArrayList<File>x){StringBuilder b=new StringBuilder();for(File f:x)b.append(f.getAbsolutePath()).append('\n');return b.toString();}
    String ext(File f){String n=f.getName();int p=n.lastIndexOf('.');return p>0?n.substring(p+1):"";}
    String icon(String name){String n=name==null?"":name.toLowerCase(Locale.ROOT);int p=n.lastIndexOf('.');String e=p>0?n.substring(p+1):"";if(e.matches("jpg|jpeg|png|gif|webp|bmp"))return"🖼️";if(e.matches("mp4|mkv|avi|mov|3gp"))return"🎬";if(e.matches("mp3|wav|flac|aac|ogg"))return"🎵";if(e.matches("zip|7z|rar|tar|gz"))return"🗜️";if(e.matches("pdf"))return"📕";if(e.matches("apk|xapk|apks|aab"))return"📦";return"📄";}
    String icon(File f){return icon(f.getName());}
    String human(long n){if(n<1024)return n+" B";double x=n;String[]u={"KB","MB","GB","TB"};int i=0;while(x>=1024&&i<u.length-1){x/=1024;i++;}return String.format(Locale.US,"%.1f %s",x,u[i]);}
    void toast(String s){Toast.makeText(this,s==null?"Error":s,Toast.LENGTH_LONG).show();}
}
