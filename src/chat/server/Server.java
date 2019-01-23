package chat.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chat.Log;
import chat.server.ServerThread;


public class Server {
    private ServerSocket ss;

    private ArrayList<ServerThread> cList;

    private ExecutorService executorService;
    
    public Server() {
        try {
            ss = new ServerSocket(9999);
            System.out.println("Server Start!");
            cList = new ArrayList <>();
            executorService = Executors.newFixedThreadPool(3);
        } catch (IOException ex) {
            System.out.println("���� ���� ����!");
            ex.printStackTrace();
        }
    }

    public void execute() {
        while(true) {
            try {
              Socket s = ss.accept();
              ServerThread ct = new ServerThread(s,this);
              cList.add(ct);
              Log.info("Current number of Clients:"+cList.size());
              executorService.submit(ct);
              //Thread t = new Thread(ct);
              //t.start();
            } catch (IOException ex) {
              ex.printStackTrace();
            }        
        }
    }
    public static void main(String[] args) {
    	Thread.currentThread().setName("T-SERVER");
       Server server = new Server();
        server.execute();
    }
    public void sendMessage(String clientMsg) {
        for(ServerThread e : cList) {
            e.gewPw().println(clientMsg);
        }
    }

	public List<ServerThread> getChatter() {
		return this.cList;
	}

	public void removeChatter(ServerThread chatter) {
		if ( cList.remove(chatter) ) {
			Log.info("REMOVED: " + chatter.nickname);
		} else {
			Log.info("FAIL TO REMOVE : " + chatter.nickname);
		}
		
	}

	public void boradcastMessage(ServerThread sender, String text) {
		List<ServerThread> chatters = getChatter();
    	Log.info("BR-MESSAGE: " + chatters.size() + " clients");
    	for (ServerThread client : chatters) {
    		try {
    			client.sendMessage(sender.nickname, text);
    		} catch (Exception e) {
    			Log.info("���� �ȵ�! : " + client.nickname);
    			e.printStackTrace();
    		}
		}
	}
}
