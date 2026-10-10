package com.hfpvoipfix;
import android.content.*;import android.database.*;import android.net.Uri;
import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;
import java.io.*;
public final class ReportProvider extends ContentProvider {
  public static final Uri URI=Uri.parse("content://com.hfpvoipfix.report/rapport");
  public static final Uri FIRMWARE=Uri.parse("content://com.hfpvoipfix.report/firmware");
  private String filename(Uri u){return "/rapport".equals(u.getPath())?"HFP-Rapport.zip":"/firmware".equals(u.getPath())?"HFP-Firmware.zip":null;}
  @Override public boolean onCreate(){return true;}
  @Override public String getType(Uri u){return "application/zip";}
  @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String order){
    if(filename(u)==null)return null;
    File f=new File(getContext().getFilesDir(),filename(u));
    MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});
    c.addRow(new Object[]{filename(u),f.isFile()?f.length():0});return c;
  }
  @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{
    if(filename(u)==null||!"r".equals(mode))throw new FileNotFoundException();
    return ParcelFileDescriptor.open(new File(getContext().getFilesDir(),filename(u)),ParcelFileDescriptor.MODE_READ_ONLY);
  }
  @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
  @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
  @Override public int delete(Uri u,String s,String[] a){return 0;}
}
