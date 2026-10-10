package com.hfpvoipfix;
import java.io.*;
public final class ProbeChannelTest {
    static class Pipe extends java.lang.Process {
        final PipedInputStream appRead=new PipedInputStream(65536),shellRead=new PipedInputStream(65536);
        final PipedOutputStream shellWrite,appWrite;boolean destroyed;
        Pipe()throws Exception{shellWrite=new PipedOutputStream(appRead);appWrite=new PipedOutputStream(shellRead);}
        public OutputStream getOutputStream(){return appWrite;}public InputStream getInputStream(){return appRead;}public InputStream getErrorStream(){return new ByteArrayInputStream(new byte[0]);}
        public int waitFor(){return 0;}public int exitValue(){return destroyed?0:1;}public void destroy(){destroyed=true;try{appWrite.close();appRead.close();shellWrite.close();}catch(IOException ignored){}}
    }
    static Pipe[] start(boolean fail)throws Exception{
        Pipe shell=new Pipe(),log=new Pipe();
        new Thread(()->{try(BufferedReader r=new BufferedReader(new InputStreamReader(shell.shellRead));PrintWriter w=new PrintWriter(shell.shellWrite,true);PrintWriter event=new PrintWriter(log.shellWrite,true)){String s;
            while((s=r.readLine())!=null){if(s.startsWith("echo UID"))w.println(s.substring(5,s.indexOf("$("))+"0");
                else if(s.startsWith("setprop")){String token=s.split(" ")[2];w.println((fail?"FAIL":"OK")+token);
                    for(int i=0;i<1500;i++)event.println("Noise logcat message "+i);
                    event.println("I/HfpVoipFix: LAB epoch=42 STATUS version=1.7.7 token=old busy=false");
                    if(!fail)event.println("I/HfpVoipFix: LAB epoch=42 STATUS version=1.7.7 token="+token+" busy=false");
                }
            }
        }catch(IOException ignored){}}).start();return new Pipe[]{shell,log};
    }
    public static void main(String[] args)throws Exception{
        Pipe[] p=start(false);String previous="";
        try(ProbeChannel c=new ProbeChannel(p[0],p[1])){for(int i=0;i<100;i++){String line=c.read();if(line.contains("token=old")||line.equals(previous))throw new AssertionError("stale heartbeat");previous=line;}}
        if(!p[0].destroyed||!p[1].destroyed)throw new AssertionError("cleanup");
        Pipe[] denied=start(true);try(ProbeChannel c=new ProbeChannel(denied[0],denied[1])){try{c.read();throw new AssertionError("setprop failure accepted");}catch(IOException expected){if(!expected.getMessage().contains("setprop"))throw expected;}}
        if(ProbeChannel.matching("LAB epoch=42 STATUS version=1.7.7 token=abc123 busy=false","abc")!=null)throw new AssertionError("prefix match");
        if(ProbeChannel.matching("LAB SOFT_BRIDGE HANDSHAKE STATUS version=1.7.7 token=abc busy=false","abc")!=null)throw new AssertionError("app replay matched");
        System.out.println("PASS live status: 100 fresh handshakes amid 150000 unrelated lines, refusal and dual-process cleanup");
    }
}
