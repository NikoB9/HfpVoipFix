package com.hfpvoipfix;
/** Gray-code order: only one audio choice changes between adjacent comparisons. */
final class AaAudioTrial {
 static final int COUNT=4;
 final int index,usage,micSource;
 final String name;
 private AaAudioTrial(int i,int u,int m,String n){index=i;usage=u;micSource=m;name=n;}
 static AaAudioTrial at(int index){
  switch(index){
   case 1:return new AaAudioTrial(1,1,7,"Média · micro communication");
   case 2:return new AaAudioTrial(2,1,1,"Média · micro standard");
   case 3:return new AaAudioTrial(3,12,1,"Navigation · micro standard");
   default:return new AaAudioTrial(0,12,7,"Navigation · micro communication");
  }
 }
 static boolean continueTrial(boolean enabled,int index,boolean started){return enabled&&index>=0&&index<COUNT&&started;}
 static int select(boolean enabled,int next,String previous,int assigned,String call){
  if(!enabled||call==null||!call.matches("[0-9]+:[1-9][0-9]*"))return -1;
  if(call.equals(previous)&&assigned>=0&&assigned<COUNT)return assigned;
  return next>=0&&next<COUNT?next:-1;
 }
}
