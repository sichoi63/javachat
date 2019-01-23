
package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import chat.Log;


public class ServerThread implements Runnable{
    private Socket socket;
    private Server server;
    private BufferedReader br;
    private PrintWriter pw;
    
    String nickname;
    
    public PrintWriter gewPw() {
        return pw;
    }
    
    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        Log.info("IP:"+ socket.getInetAddress().getHostAddress());
    }

    void broadcastNewJoiner(String nickName) {
    	List<ServerThread> chatters = server.getChatter();
    	Log.info("BR " + chatters.size() + " clients");
    	for (ServerThread client : chatters) {
    		
    		try {
    			if ( !client.nickname.equals(nickName) ) {
    				client.notifyNewJoiner(nickName);    			    				
    			}
    		} catch (Exception e) {
    			Log.info("연결 안됨! : " + client.nickname);
    			e.printStackTrace();
    		}
		}
    }
    
    public void broadcastLogout(String nickname) {
    	List<ServerThread> chatters = server.getChatter();
    	Log.info("BR-LOGOUT " + chatters.size() + " clients");
    	for (ServerThread client : chatters) {
    		
    		try {
    			client.notifyLogout(nickname);
    		} catch (Exception e) {
    			Log.info("연결 안됨! : " + client.nickname);
    			e.printStackTrace();
    		}
		}
		
	}
    
    private void notifyLogout(String logouter) {
    	String msg = "CHATTER-EXIT|" + logouter;
    	pw.println(msg);
    	Log.info(msg);
	}

	void notifyNewJoiner(String joiner) {
    	String msg = "JOINED|" + joiner.trim();
		this.pw.println("JOINED|" + joiner.trim());
		this.pw.flush();
		Log.info(msg);
		
	}

	@Override
    public void run() {
        try {
//        	new PrintWriter
//            pw = new PrintWriter(socket.getOutputStream(),true);
        	
        	OutputStream os = socket.getOutputStream();
        	pw = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);
            br = new BufferedReader(
                   new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
          while (true) {
              String clientMsg = br.readLine(); // LOGIN|, 
              Log.info("SERVER-THREAD:" + clientMsg);
              String [] params = clientMsg.split("\\|");
              
              if ( "LOGIN".equals(params[0].trim())) {
            	  // LOGIN|jack
            	  String joiner = params[1].trim();
            	  this.nickname = joiner;
            	  broadcastNewJoiner(joiner); // 기존 참여자들에게
            	  sendChatters();
              } else if ( "MSG".equals(params[0].trim()) ) {
            	  //  server.sendMessage(clientMsg);
            	  ServerThread sender = this;
            	  String text = params[1].trim();
            	  server.boradcastMessage(sender, text);
              }else if("LOGOUT".equals(params[0].trim())) {
            	  server.removeChatter(this);
            	  broadcastLogout(this.nickname);
              }
              else {
					String error = String.format("알 수 없는 명령어 : %s ( 전문 : %s)", params[0].trim(), clientMsg);
					throw new RuntimeException(error);
			}
              
          }
          
        } catch (IOException ex) {   
        }
    }

	void sendChatters() {
		String msg = "CHATTERS|";
		List<ServerThread> chatters = server.getChatter();
		List<String> nick = new ArrayList<>();
		for (ServerThread each : chatters) {
			nick.add(each.nickname);
		}
		
		// A,B,C
		String [] n = nick.toArray(new String[nick.size()]);
		String nickList = String.join(",", n);
		msg += nickList; //CHATTERS|A,B,C,D
		pw.println(msg);
		pw.flush();
		Log.info(msg);
		
		// java8 stream()
		// List<String> nick2 = chatters.stream().map(each -> each.nickname).collect(Collectors.toList());
	}

	public void sendMessage(String sender, String text) {
		String msg = String.format("MSG|%s|%s", sender, text);
		pw.println(msg);
		pw.flush();
		Log.info(msg);
		
	}
}
