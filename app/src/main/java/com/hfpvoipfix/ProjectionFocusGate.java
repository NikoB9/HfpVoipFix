package com.hfpvoipfix;

/** Explicit experimental coexistence policy for the two ends of the same D6 relay. */
final class ProjectionFocusGate {
 static boolean allow(boolean liveTransport,String loser,int loserUsage,int loserContent,int loserGain,
                      int previousLoss,String winner,int winnerUsage,int winnerGain,int winnerFlags,
                      int loss,boolean forceDuck){
  return liveTransport&&"com.hfpvoipfix".equals(loser)&&loserUsage==12&&loserContent==1&&loserGain==3
   &&previousLoss==0&&"com.google.android.projection.gearhead".equals(winner)
   &&winnerGain==2&&(winnerFlags&4)==0&&!forceDuck&&loss==-2
   // AA does not publish the attributes of its focus commands in the report.
   // Permit call/media/navigation/default proxy attributes; exclude assistant,
   // accessibility, alarms, notifications and exclusive/permanent requests.
   &&(winnerUsage==0||winnerUsage==1||winnerUsage==2||winnerUsage==12);
 }
}
