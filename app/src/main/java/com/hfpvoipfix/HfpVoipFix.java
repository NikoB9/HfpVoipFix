package com.hfpvoipfix;

import android.media.AudioManager;
import android.os.Process;
import android.os.SystemClock;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.util.Log;
import java.lang.reflect.Array;
import java.util.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Hooks stay installed; a strategy is latched for the entire call/SCO lifetime. */
public final class HfpVoipFix implements IXposedHookLoadPackage {
    private static final String TAG="HfpVoipFix", BASE="com.android.bluetooth.hfpclient.";
    private static final Object LOCK=new Object();
    private static final Set<Object> calls=new HashSet<>();
    private static final Set<Object> scos=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
    private static final Map<Object,Integer> callIds=new HashMap<>();
    private static int nextCallId=1;
    private static final Map<Object,Object> callMachines=new HashMap<>();
    private static boolean disconnectHook, bypassConfirmed, commAttempted, commRequested, commCleanupUnconfirmed;
    private static AudioManager commAudio;
    private static Object pendingCommMachine;
    private static int lastCommMode=-1;
    private static String outcome="idle";
    private static String selected="observe", previousWbs=null, previousBtSco=null, lastError="aucune";
    private static Object owner, rxPatch, txPatch;
    private static boolean addedIn,addedOut, installed, routeHook, callHook, bypassHook;
    private static boolean mtkModeApplied, mtkModePending, mtkScoApplied;
    private static int voipHooks;
    private static Class<?> controlService;
    private static String controlToken="none",controlResult="idle",controlDetail="none";
    private static long scoSequence;
    private static int negotiatedRate;
    private static boolean softwarePorts;
    private static String address="";
    private static final int OUT_SCO=0x20, IN_SCO=0x80000008, SPEAKER=2, MIC=0x80000004;
    private static final long epoch=SystemClock.elapsedRealtime();
    private static String prop(String key,String fallback){
        try{return (String)XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",null),"get",key,fallback);}
        catch(Throwable t){return fallback;}
    }
    private static void event(String s){if(s.startsWith("UNAVAILABLE")||s.startsWith("ERROR"))outcome="unavailable";Log.i(TAG,"LAB epoch="+epoch+" "+s);}
    private static void error(String where,Throwable t){outcome="unavailable";lastError=where+":"+t.getClass().getSimpleName();Log.e(TAG,"LAB ERROR "+lastError);}
    private static boolean busy(){return !calls.isEmpty()||!scos.isEmpty();}
    private static void latch(){if(!busy()){
        selected=LabModes.valid(prop(LabModes.PROP,"observe"));
        bypassConfirmed=false;commAttempted=false;pendingCommMachine=null;mtkModePending=false;strategy("pending","waiting_for_next_route");
        if(mtkModeApplied||mtkScoApplied){event("UNAVAILABLE mode=software_bridge reason=previous_mtk_state_not_restored");selected="observe";outcome="unavailable";}
        if(commRequested||commCleanupUnconfirmed){event("UNAVAILABLE mode="+selected+" reason=communication_cleanup_pending");selected="observe";outcome="unavailable";}
        if(owner!=null&&!scos.contains(owner)){
            event("UNAVAILABLE mode="+selected+" reason=cleanup_pending");selected="observe";
        }
        if(!selected.equals("observe")&&(!callHook||!routeHook)){
            event("UNAVAILABLE mode="+selected+" reason=lifecycle_hooks_missing");selected="observe";
        }
        if(LabModes.bypass(selected)&&!bypassHook){event("UNAVAILABLE mode=bypass reason=hook_missing");selected="observe";}
        if((selected.equals("bypass_comm")||LabModes.softwareBridge(selected))&&!disconnectHook){event("UNAVAILABLE mode=bypass_comm reason=disconnect_hook_missing");selected="observe";outcome="unavailable";}
        if(LabModes.voip(selected)&&voipHooks!=3){event("UNAVAILABLE mode="+selected+" reason=voip_hooks_missing");selected="observe";}
    }}
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
        if(!"com.android.bluetooth".equals(p.packageName)||installed)return;
        installed=true;
        ClassLoader cl=p.classLoader;
        try{Class<?> sm=XposedHelpers.findClass(BASE+"HeadsetClientStateMachine",cl);
            XposedHelpers.findAndHookMethod(sm,"routeHfpAudio",boolean.class,new RouteHook());routeHook=true;
            Class<?> call=XposedHelpers.findClass("android.bluetooth.BluetoothHeadsetClientCall",cl);
            XposedHelpers.findAndHookMethod(sm,"sendCallChangedIntent",call,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){synchronized(LOCK){try{
                    Object c=p.args[0], key=XposedHelpers.callMethod(c,"getUUID");
                    int state=(Integer)XposedHelpers.callMethod(c,"getState");
                    if(state!=7){latch();calls.add(key);callMachines.put(key,p.thisObject);}
                    if(!callIds.containsKey(key))callIds.put(key,nextCallId++);
                    event("CALL id="+callIds.get(key)+" state="+state+" mode="+selected+" ms="+System.currentTimeMillis());
                    if(state==7){calls.remove(key);callIds.remove(key);callMachines.remove(key);finishCommunicationIfIdle();}
                }catch(Throwable t){error("call_state",t);}}}
            });callHook=true;
        }catch(Throwable t){error("lifecycle_hook",t);}
        try{
            XposedHelpers.findAndHookMethod(XposedHelpers.findClass(BASE+"HeadsetClientStateMachine",cl),
                "broadcastConnectionState",XposedHelpers.findClass("android.bluetooth.BluetoothDevice",cl),int.class,int.class,new XC_MethodHook(){
                    @Override protected void afterHookedMethod(MethodHookParam p){if(!Integer.valueOf(0).equals(p.args[1]))return;
                        synchronized(LOCK){
                            for(Object key:new ArrayList<>(callMachines.keySet()))if(callMachines.get(key)==p.thisObject){
                                event("CALL id="+callIds.get(key)+" state=7 mode="+selected+" ms="+System.currentTimeMillis()+" reason=profile_disconnected");
                                calls.remove(key);callIds.remove(key);callMachines.remove(key);
                            }
                            scos.remove(p.thisObject);
                            if(owner==p.thisObject)cleanup();
                            if(pendingCommMachine==p.thisObject)pendingCommMachine=null;
                            event("PROFILE_DISCONNECTED mode="+selected);finishCommunicationIfIdle();
                        }
                    }
                });disconnectHook=true;
        }catch(Throwable t){error("disconnect_hook_missing",t);}
        try{Class<?> block=XposedHelpers.findClass(BASE+"connserv.HfpClientDeviceBlock",cl);
            block.getDeclaredField("mConnections");
            Class<?> call=XposedHelpers.findClass("android.bluetooth.BluetoothHeadsetClientCall",cl);
            XposedHelpers.findAndHookMethod(block,"handleCall",call,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam p){synchronized(LOCK){
                    if(!LabModes.bypass(selected))return;
                    try{
                        Object connections=XposedHelpers.getObjectField(p.thisObject,"mConnections");
                        if(!(connections instanceof Map)||!((Map<?,?>)connections).isEmpty()){
                            event("UNAVAILABLE mode=bypass reason=existing_telecom_connections");pendingCommMachine=null;releaseCommunication();selected="observe";return;
                        }
                        p.setResult(null);
                        if(!Integer.valueOf(7).equals(XposedHelpers.callMethod(p.args[0],"getState")))bypassConfirmed=true;
                        event("BYPASS_APPLIED handleCall Telecom_not_registered");
                        if(selected.equals("bypass"))strategy("applied","telecom_bypass");
                        if(pendingCommMachine!=null)activateCommunication(pendingCommMachine,"after_bypass");
                    }catch(Throwable t){error("bypass_unavailable",t);pendingCommMachine=null;releaseCommunication();selected="observe";}
                }}
            });bypassHook=true;
        }catch(Throwable t){error("bypass_hook_missing",t);}
        for(String method:new String[]{"onCreateIncomingConnection","onCreateOutgoingConnection","onCreateUnknownConnection"}){
            try{XposedHelpers.findAndHookMethod(XposedHelpers.findClass(BASE+"connserv.HfpClientConnectionService",cl),method,
                XposedHelpers.findClass("android.telecom.PhoneAccountHandle",null),
                XposedHelpers.findClass("android.telecom.ConnectionRequest",null),new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){synchronized(LOCK){latch();
                        if(LabModes.bypass(selected)){
                            p.setResult(Connection.createFailedConnection(new DisconnectCause(DisconnectCause.CANCELED)));
                            event("BYPASS_REFUSE_LOCAL_TELECOM_CONNECTION use_Samsung");
                        }
                    }}
                    @Override protected void afterHookedMethod(MethodHookParam p){synchronized(LOCK){
                        if(p.hasThrowable()||p.getResult()==null||!LabModes.voip(selected))return;
                        try{XposedHelpers.callMethod(p.getResult(),"setAudioModeIsVoip",true);event("VOIP_APPLIED mode="+selected);strategy(selected.equals("voip")?"applied":"partial","voip_connection_flag_set");}
                        catch(Throwable t){error("early_voip",t);}
                    }}
                });voipHooks++;
            }catch(Throwable t){error("hook_"+method,t);}
        }
        try{Class<?> service=XposedHelpers.findClass(BASE+"HeadsetClientService",cl),device=XposedHelpers.findClass("android.bluetooth.BluetoothDevice",cl);
            service.getDeclaredMethod("getHeadsetClientService");service.getDeclaredMethod("getConnectedDevices");
            service.getDeclaredMethod("dial",device,String.class);service.getDeclaredMethod("acceptCall",device,int.class);service.getDeclaredMethod("terminateCall",device,java.util.UUID.class);
            controlService=service;}catch(Throwable t){error("control_service_missing",t);}
        event("HOOKS version=1.7.10 route="+routeHook+" calls="+callHook+" bypass="+bypassHook+" voip="+voipHooks);
        // Read-only root-property handshake. No Bluetooth-process property writes or app context needed.
        Timer timer=new Timer("HfpLab-status",true);
        timer.scheduleAtFixedRate(new TimerTask(){String previous="",previousControl=prop("debug.hfpvoipfix.ctrl","");public void run(){
            String command=prop("debug.hfpvoipfix.ctrl","");
            if(!command.equals(previousControl)){previousControl=command;executeControl(command);}
            synchronized(LOCK){monitorCommunication();}
            String token=prop("debug.hfpvoipfix.probe","");
            if(!token.matches("[a-zA-Z0-9]{1,32}")||token.equals(previous))return;
            previous=token;synchronized(LOCK){event("STATUS version=1.7.10 token="+token+" pid="+Process.myPid()+" epoch="+epoch+
                " ctrl_token="+controlToken+" ctrl_result="+controlResult+" ctrl_error="+controlDetail+" control="+(controlService!=null)+
                " requested="+LabModes.valid(prop(LabModes.PROP,"observe"))+" applied="+(busy()?selected:"idle")+
                " sco="+scos.size()+" sco_seq="+scoSequence+" rate="+negotiatedRate+" bypass_ok="+bypassConfirmed+" soft_ports="+softwarePorts+
                " outcome="+outcome+" disconnect="+disconnectHook+" comm="+commRequested+" busy="+busy()+" dirty="+((owner!=null&&!scos.contains(owner))||(commRequested&&!busy())||commCleanupUnconfirmed)+" route="+routeHook+" calls="+callHook+" bypass="+bypassHook+" voip="+voipHooks+" error="+lastError);}
        }},500,500);
    }
    private static void executeControl(String raw){String[] p=CallCommand.parse(raw);if(p==null)return;
        synchronized(LOCK){if(p[0].equals(controlToken))return;controlToken=p[0];controlResult="refused";controlDetail="none";
            try{
                if(controlService==null)throw new IllegalStateException("service_missing");
                String mode=busy()?selected:LabModes.valid(prop(LabModes.PROP,"observe"));
                if(!LabModes.bypass(mode))throw new IllegalStateException("bypass_mode_required");
                if(!bypassHook||!callHook||!routeHook||!disconnectHook)throw new IllegalStateException("hooks_missing");
                if(commCleanupUnconfirmed||(owner!=null&&!scos.contains(owner)))throw new IllegalStateException("cleanup_pending");
                Object service=XposedHelpers.callStaticMethod(controlService,"getHeadsetClientService");
                Object devices=XposedHelpers.callMethod(service,"getConnectedDevices");
                if(!(devices instanceof List)||((List<?>)devices).size()!=1)throw new IllegalStateException("exactly_one_HFP_device_required");
                Object device=((List<?>)devices).get(0),result;
                if(p[1].equals("dial")){
                    if(busy())throw new IllegalStateException("call_already_active");
                    result=XposedHelpers.callMethod(service,"dial",device,p[2]);controlResult=result==null?"rejected":"queued";
                }else if(p[1].equals("answer")){
                    result=XposedHelpers.callMethod(service,"acceptCall",device,0);controlResult=Boolean.TRUE.equals(result)?"queued":"rejected";
                }else{
                    result=XposedHelpers.callMethod(service,"terminateCall",device,(Object)null);controlResult=Boolean.TRUE.equals(result)?"queued":"rejected";
                }
            }catch(Throwable t){controlResult="unavailable";String message=t.getMessage();controlDetail=message!=null&&message.matches("[a-z_]+")?message:"service_call_failed";event("CONTROL_ERROR type="+t.getClass().getSimpleName()+" reason="+controlDetail);}
            event("CONTROL token="+controlToken+" action="+p[1]+" result="+controlResult+" remote_phone_only");
        }
    }
    private static void strategy(String result,String detail){outcome=result;if(result.equals("unavailable"))lastError=detail;event("STRATEGY mode="+selected+" result="+result+" detail="+detail);}
    private static void activateCommunication(Object sm,String timing){
        if(!selected.equals("bypass_comm")||commAttempted||!scos.contains(sm))return;
        if(!bypassConfirmed){event("COMM_WAIT reason=bypass_not_yet_confirmed");return;}
        commAttempted=true;
        try{
            if(scos.size()!=1){strategy("unavailable","multiple_sco_routes");return;}
            Object manager=XposedHelpers.getObjectField(sm,"mAudioManager");
            if(!(manager instanceof AudioManager)){strategy("unavailable","audio_manager_missing");return;}
            AudioManager a=(AudioManager)manager;int before=a.getMode();
            event("COMM_BEFORE actual_mode="+before+" timing="+timing);
            if(before!=AudioManager.MODE_NORMAL){strategy("unavailable","audio_mode_already_owned_"+before);return;}
            commAudio=a;commRequested=true;
            a.setMode(AudioManager.MODE_IN_COMMUNICATION);
            lastCommMode=a.getMode();event("COMM_REQUEST requested=3 actual_mode="+lastCommMode);
            if(lastCommMode==AudioManager.MODE_IN_COMMUNICATION)strategy("applied","bypass_and_communication_confirmed");
            else {strategy("unavailable","communication_request_not_effective");releaseCommunication();}
        }catch(Throwable t){error("communication_request",t);releaseCommunication();}
    }
    private static void monitorCommunication(){
        if(!commRequested||commAudio==null)return;
        try{int actual=commAudio.getMode();if(actual!=lastCommMode){lastCommMode=actual;
            event("COMM_MONITOR actual_mode="+actual);
            if(actual!=AudioManager.MODE_IN_COMMUNICATION)strategy("unavailable","communication_mode_lost");
        }}catch(Throwable t){error("communication_monitor",t);}
    }
    private static void finishCommunicationIfIdle(){if(!busy()){pendingCommMachine=null;releaseCommunication();}}
    private static void releaseCommunication(){
        if(!commRequested||commAudio==null)return;
        try{
            // Android 11: NORMAL removes this PID's mode request; another caller's request remains.
            commAudio.setMode(AudioManager.MODE_NORMAL);
            int actual=commAudio.getMode();event("COMM_RELEASE requested=0 actual_mode="+actual);
            if(actual==AudioManager.MODE_IN_COMMUNICATION){
                event("ERROR communication_release_unconfirmed");lastError="communication_release_unconfirmed";outcome="unavailable";commCleanupUnconfirmed=true;
            }
            commRequested=false;commAudio=null;lastCommMode=-1;
        }catch(Throwable t){error("communication_release",t);}
    }
    private static Class<?> audio(){return XposedHelpers.findClass("android.media.AudioSystem",null);}
    private static int status(Object x){return x instanceof Integer?(Integer)x:-1;}
    private static int params(String s){int r=status(XposedHelpers.callStaticMethod(audio(),"setParameters",s));event("PARAM "+s+" status="+r);return r;}
    private static String getWbs(){
        Object raw=XposedHelpers.callStaticMethod(audio(),"getParameters","bt_wbs");
        for(String item:String.valueOf(raw).split(";")){if(item.trim().matches("bt_wbs=(on|off)"))return item.trim();}
        return null;
    }
    private static void codec(Object sm){
        Boolean wbs=null;
        try{Object value=XposedHelpers.getObjectField(sm,"mAudioWbs");if(value instanceof Boolean)wbs=(Boolean)value;}
        catch(Throwable t){error("negotiated_wbs_unreadable",t);}
        negotiatedRate=wbs==null?0:(wbs?16000:8000);
        event("CODEC negotiated_wbs="+wbs+" mode="+selected+" no_codec_renegotiation");
        if(LabModes.softwareBridge(selected)){
            mtkModePending=false;
            if(wbs==null){event("UNAVAILABLE mtk_wbs reason=negotiated_codec_unreadable");return;}
            if(!wbs){event("MTK_WBS_SKIP reason=narrowband_negotiated");return;}
            String old=null;
            try{old=getWbs();}catch(Throwable t){error("read_mtk_wbs",t);}
            if(old==null){previousWbs="bt_wbs=off";event("MTK_WBS_BASELINE source=prior_RX_TRACE assumed=bt_wbs_off");}
            else{previousWbs=old;event("MTK_WBS_BASELINE source=HAL_GET value="+old);}
            // The HAL's BT_SCO parameter is not readable through getParameters.
            // D2 runs only while no call/SCO was active, so restore the observed idle default.
            previousBtSco="BT_SCO=off";
            event("MTK_SCO_BASELINE source=idle_default assumed=BT_SCO_off");
            mtkModePending=true;
            event("MTK_WBS_DEFER negotiated_wbs=true stage=after_native_route");
            return;
        }
        if(!selected.equals("wbs")&&!selected.equals("codec"))return;
        if(wbs==null||(selected.equals("wbs")&&!wbs)){event("UNAVAILABLE codec_write reason=negotiated_wbs");return;}
        try{String old=getWbs();if(old==null){event("UNAVAILABLE codec_write reason=previous_HAL_value_unreadable");return;}
            if(params("bt_wbs="+(wbs?"on":"off"))==0)previousWbs=old;
        }catch(Throwable t){error("codec_write",t);}
    }
    private static boolean connectPort(int type){
        int current=status(XposedHelpers.callStaticMethod(audio(),"getDeviceConnectionState",type,address));
        if(current!=0){event("PORT_SKIP type="+type+" existing="+current);return false;}
        int r=status(XposedHelpers.callStaticMethod(audio(),"setDeviceConnectionState",type,1,address,"HfpClient SCO",0));
        event("PORT_ADD type="+type+" status="+r);return r==0;
    }
    private static Object findPort(List<Object> ports,int type){for(Object p:ports)try{
        if(status(XposedHelpers.callMethod(p,"type"))==type){
            if(type==IN_SCO||type==OUT_SCO){if(!address.equals(String.valueOf(XposedHelpers.callMethod(p,"address"))))continue;}
            return p;
        }
    }catch(Throwable ignored){}return null;}
    private static Object array(String cls,Object value){Object a=Array.newInstance(XposedHelpers.findClass(cls,null),1);Array.set(a,0,value);return a;}
    private static Object patch(List<Object> ports,int from,int to,String direction){
        Object source=findPort(ports,from), sink=findPort(ports,to);
        if(source==null||sink==null){event("UNAVAILABLE patch="+direction+" reason=missing_ports");return null;}
        Object sources=array("android.media.AudioPortConfig",XposedHelpers.callMethod(source,"buildConfig",0,0,0,null));
        Object sinks=array("android.media.AudioPortConfig",XposedHelpers.callMethod(sink,"buildConfig",0,0,0,null));
        Object patches=Array.newInstance(XposedHelpers.findClass("android.media.AudioPatch",null),1);
        int r=status(XposedHelpers.callStaticMethod(AudioManager.class,"createAudioPatch",patches,sources,sinks));
        event("PATCH direction="+direction+" status="+r+" transport_not_verified");return r==0?Array.get(patches,0):null;
    }
    private static void release(){
        if(rxPatch!=null)try{int r=status(XposedHelpers.callStaticMethod(AudioManager.class,"releaseAudioPatch",rxPatch));event("RELEASE_RX status="+r);if(r==0)rxPatch=null;}
            catch(Throwable t){error("release_rx",t);}
        if(txPatch!=null)try{int r=status(XposedHelpers.callStaticMethod(AudioManager.class,"releaseAudioPatch",txPatch));event("RELEASE_TX status="+r);if(r==0)txPatch=null;}
            catch(Throwable t){error("release_tx",t);}
        if(mtkModeApplied)try{String restore=previousWbs==null?"bt_wbs=off":previousWbs;int r=params(restore);if(r==0){mtkModeApplied=false;previousWbs=null;event("MTK_WBS_RESTORED value="+restore);}
            else{lastError="mtk_mode_restore_status_"+r;event("ERROR mtk_mode_restore_failed status="+r);}}
            catch(Throwable t){error("restore_mtk_mode",t);}
        if(mtkScoApplied)try{String restore=previousBtSco==null?"BT_SCO=off":previousBtSco;int r=params(restore);if(r==0){mtkScoApplied=false;previousBtSco=null;event("MTK_SCO_RESTORED value="+restore);}
            else{lastError="mtk_sco_restore_status_"+r;event("ERROR mtk_sco_restore_failed status="+r);}}
            catch(Throwable t){error("restore_mtk_sco",t);}
        mtkModePending=false;
    }
    private static void cleanup(){
        softwarePorts=false;
        release();
        if(!mtkScoApplied)previousBtSco=null;
        if(rxPatch!=null||txPatch!=null){event("ERROR cleanup_incomplete patches_retained intervention_disabled");return;}
        if(addedIn)try{int r=status(XposedHelpers.callStaticMethod(audio(),"setDeviceConnectionState",IN_SCO,0,address,"HfpClient SCO",0));event("PORT_REMOVE_IN status="+r);if(r==0)addedIn=false;}catch(Throwable t){error("remove_in",t);}
        if(addedOut)try{int r=status(XposedHelpers.callStaticMethod(audio(),"setDeviceConnectionState",OUT_SCO,0,address,"HfpClient SCO",0));event("PORT_REMOVE_OUT status="+r);if(r==0)addedOut=false;}catch(Throwable t){error("remove_out",t);}
        if(previousWbs!=null)try{if(params(previousWbs)==0){previousWbs=null;mtkModeApplied=false;}}catch(Throwable t){error("restore_wbs",t);}
        if(!addedIn&&!addedOut&&previousWbs==null&&previousBtSco==null&&!mtkModeApplied&&!mtkScoApplied)owner=null;
    }
    private static final class RouteHook extends XC_MethodHook {
        @Override protected void beforeHookedMethod(MethodHookParam p){synchronized(LOCK){
            boolean enable=Boolean.TRUE.equals(p.args[0]);
            if(enable){
                if(scos.contains(p.thisObject)){p.setObjectExtra("lab.duplicate",true);return;}
                latch();scos.add(p.thisObject);scoSequence++;softwarePorts=false;negotiatedRate=0;
                event("SCO_BEGIN mode="+selected+" ms="+System.currentTimeMillis());
                if(owner!=null){p.setObjectExtra("lab.skip",true);event("UNAVAILABLE intervention reason=owner_or_cleanup_pending");return;}
                owner=p.thisObject;
                try{
                    codec(p.thisObject);
                    if(selected.equals("observe"))strategy("observation","native_routing_only");
                    if(selected.equals("bypass_comm")){pendingCommMachine=p.thisObject;activateCommunication(p.thisObject,"before_native_route");}
                    if(LabModes.ports(selected)){
                        Object dev=XposedHelpers.getObjectField(p.thisObject,"mCurrentDevice");
                        address=(String)XposedHelpers.callMethod(dev,"getAddress");
                        if(LabModes.softwareBridge(selected)){
                            if(!mtkModePending){softwarePorts=false;strategy("unavailable","mtk_wbs_not_pending");}
                            else{int softwareDevice=selected.equals("soft_tx")?OUT_SCO:IN_SCO;
                                if(selected.equals("soft_tx"))addedOut=connectPort(OUT_SCO);else addedIn=connectPort(IN_SCO);
                                softwarePorts=status(XposedHelpers.callStaticMethod(audio(),"getDeviceConnectionState",softwareDevice,address))!=0;
                                String direction=selected.equals("soft_tx")?"tx":"rx";
                                strategy(softwarePorts?"partial":"unavailable",softwarePorts?"software_"+direction+"_port_ready_waiting_for_app":"software_"+direction+"_port_missing");}
                        }else{addedOut=connectPort(OUT_SCO);addedIn=connectPort(IN_SCO);}
                    }
                }catch(Throwable t){error("route_before",t);}
            }else if(p.thisObject==owner)release();
        }}
        @Override protected void afterHookedMethod(MethodHookParam p){synchronized(LOCK){
            boolean enable=Boolean.TRUE.equals(p.args[0]);
            if(enable){
                if(Boolean.TRUE.equals(p.getObjectExtra("lab.duplicate"))||Boolean.TRUE.equals(p.getObjectExtra("lab.skip"))||p.thisObject!=owner)return;
                if(p.hasThrowable()){error("native_route",p.getThrowable());cleanup();scos.remove(p.thisObject);finishCommunicationIfIdle();return;}
                try{
                    if(LabModes.softwareBridge(selected)&&mtkModePending){
                        event("MTK_SCO_APPLY stage=after_native_route");
                        int scoResult;
                        mtkScoApplied=true;
                        try{scoResult=params("BT_SCO=on");}
                        catch(Throwable t){mtkModePending=false;softwarePorts=false;error("mtk_sco_after_route",t);cleanup();return;}
                        if(scoResult!=0){mtkScoApplied=false;previousBtSco=null;mtkModePending=false;softwarePorts=false;strategy("unavailable","mtk_sco_after_route_status_"+scoResult);cleanup();return;}
                        event("MTK_SCO_APPLIED btscoOn=true driver_bypass=off");
                        event("MTK_WBS_APPLY stage=after_native_route");
                        int result;
                        try{result=params("bt_wbs=on");}
                        catch(Throwable t){mtkModePending=false;softwarePorts=false;error("mtk_wbs_after_route",t);cleanup();return;}
                        mtkModePending=false;
                        if(result==0){mtkModeApplied=true;event("MTK_WBS_APPLIED negotiated_wbs=true stage=after_native_route restore=off");}
                        else{softwarePorts=false;strategy("unavailable","mtk_wbs_after_route_status_"+result);cleanup();return;}
                    }
                    if(!selected.equals("rx")&&!selected.equals("tx")&&!selected.equals("bridge"))return;
                    if(rxPatch!=null||txPatch!=null){event("ERROR patch_existing no_recreate");return;}
                    ArrayList<Object> ports=new ArrayList<>();
                    int r=status(XposedHelpers.callStaticMethod(AudioManager.class,"listAudioDevicePorts",ports));
                    event("PORT_LIST status="+r);if(r!=0)return;
                    if(selected.equals("rx")||selected.equals("bridge"))rxPatch=patch(ports,IN_SCO,SPEAKER,"RX");
                    if(selected.equals("tx")||selected.equals("bridge"))txPatch=patch(ports,MIC,OUT_SCO,"TX");
                }catch(Throwable t){error("create_patch",t);}
            }else{
                if(p.hasThrowable()){error("native_route_stop",p.getThrowable());return;}
                if(p.thisObject==owner)cleanup();
                if(scos.remove(p.thisObject))event("SCO_END mode="+selected+" ms="+System.currentTimeMillis());
                if(pendingCommMachine==p.thisObject)pendingCommMachine=null;
                finishCommunicationIfIdle();
            }
        }}
    }
}
