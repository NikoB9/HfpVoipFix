package com.hfpvoipfix;
/** Auto-preparation still requires a fresh idle module with matching applied choice. */
final class AutoBridgeGate {
    static boolean prepare(String probe,String mode,long age,boolean enabled,boolean running,boolean permission){
        return enabled&&!running&&permission&&age>=0&&age<4000&&"false".equals(RxGate.parse(probe).get("dirty"))&&LabModes.softwareBridge(mode)&&RxGate.armError(probe,mode,mode)==null;
    }
}
