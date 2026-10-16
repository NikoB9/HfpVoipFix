package com.hfpvoipfix;
/** Only reconciles the HAL flag with a live master lease; leaves all policy routes alone. */
final class NativeScoKeeper {
 static final int NONE=0,ENABLE=1,RESTORE=2;
 private boolean owned;
 private long revision=-1,ownedRevision=-1;
 private String session="none";
 private int failures;
 boolean owns(){return owned;}
 int action(boolean brokerOn,long observedRevision,boolean allowed,String currentSession,int audioMode){
  if(observedRevision!=revision||(allowed&&!session.equals(currentSession))){revision=observedRevision;session=currentSession;failures=0;}
  if(brokerOn||audioMode!=0){owned=false;return NONE;}
  if(failures>=2)return NONE;
  if(allowed)return !owned||ownedRevision!=observedRevision?ENABLE:NONE;
  return owned?RESTORE:NONE;
 }
 void result(int action,long observedRevision,int status){
  if(status!=0){failures++;return;}
  failures=0;owned=action==ENABLE;ownedRevision=observedRevision;
 }
}
