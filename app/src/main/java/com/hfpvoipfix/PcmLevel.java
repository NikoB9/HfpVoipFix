package com.hfpvoipfix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/** Bounded scalar observation. Never modifies or retains sample buffers. */
final class PcmLevel {
 long inspected;double energy;int peak;
 void add(Object buffer,int offset,int returned,int encoding){
  if(buffer==null||offset<0||returned<=0)return;
  if(buffer instanceof short[]&&encoding==2){short[] a=(short[])buffer;if(offset>a.length)return;int n=Math.min(returned,a.length-offset),step=Math.max(1,(int)((n+255L)/256));for(int i=0;i<n;i+=step)sample(a[offset+i]);}
  else if(buffer instanceof float[]&&encoding==4){float[] a=(float[])buffer;if(offset>a.length)return;int n=Math.min(returned,a.length-offset),step=Math.max(1,(int)((n+255L)/256));for(int i=0;i<n;i+=step){float v=a[offset+i];if(!Float.isNaN(v)&&!Float.isInfinite(v))sample((int)(Math.max(-1f,Math.min(1f,v))*32767));}}
  else if(buffer instanceof byte[]&&encoding==2){byte[] a=(byte[])buffer;if(offset>a.length)return;int n=Math.min(returned,a.length-offset)/2,step=Math.max(1,(int)((n+255L)/256));for(int i=0;i<n;i+=step){int b=offset+2*i;sample(ByteOrder.nativeOrder()==ByteOrder.LITTLE_ENDIAN?(short)((a[b]&255)|(a[b+1]<<8)):(short)((a[b]<<8)|(a[b+1]&255)));}}
  else if(buffer instanceof ByteBuffer&&encoding==2){ByteBuffer b=((ByteBuffer)buffer).duplicate();b.clear();b.order(ByteOrder.nativeOrder());int n=Math.min(returned,b.capacity())/2,step=Math.max(1,(int)((n+255L)/256));for(int i=0;i<n;i+=step)sample(b.getShort(2*i));}
 }
 private void sample(int value){inspected++;energy+=(double)value*value;peak=Math.max(peak,Math.abs(value));}
 long rms(){return inspected==0?0:(long)Math.sqrt(energy/inspected);}
}
