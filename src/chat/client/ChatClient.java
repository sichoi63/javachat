package chat.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import chat.Log;

public class ChatClient extends JFrame{

	JTextArea chatArea;  // Document 
	JTextField inputField;
	JList<String> chatters;
	private DefaultListModel<String> nicknames;
	
	private ServerConnector connector;
	
	public ChatClient() {
		this.setSize(500, 400);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.connectToServer();
		this.renderToChatPannel();
		// this.renderLoginPanel();
	}
	
	public void renderLoginPanel() {
		Container root = this.getContentPane();
		
		JPanel loginPanel = new JPanel();
		loginPanel.setLayout(new FlowLayout());
		{
			JTextField field = new JTextField("Nick");
			JButton loginButton = new JButton("login");
			loginButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					connectToServer();
				}
			});
			loginPanel.add(field);
			loginPanel.add(loginButton);
			
		}
		
		root.add(loginPanel, BorderLayout.CENTER);
		
		
	}
	protected void connectToServer() {
		try {
            Socket sock = new Socket("localhost",9999);
            // pw = new PrintWriter(sock.getOutputStream(),true);
            connector = new ServerConnector(sock);
            connector.start();
        } catch (IOException ex) {
           ex.printStackTrace();
        }
	}

	public void renderToChatPannel() {
		Container root = this.getContentPane();
		// North: logoutButton
		
		JButton btnLogout = new JButton("LOGOUT");
		btnLogout.addActionListener((e) -> processLogout());
		root.add(btnLogout, BorderLayout.NORTH);
		
		// Center :
		chatArea = new JTextArea();
		JScrollPane scroll = new JScrollPane(chatArea);
		root.add(scroll, BorderLayout.CENTER);
		
		// Soth : 
		inputField = new JTextField();
		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					processSendMessage();
				}
			}
		});
		root.add(inputField, BorderLayout.SOUTH);
		
		nicknames = new DefaultListModel<>();
		chatters = new JList<>(nicknames);
		root.add(chatters, BorderLayout.EAST);
		
		// TODO 일단 아무 이름으로 로그인 ! 
		processLogin(); 
	}
	
	void processLogout() {
		connector.sendLogout();
	}

	void processLogin() {
		String token = "FEPQAASD;DOIFKJASD;LKFJADSP;OFIAJSDPOFIASJDF;LKASDJFSAD;LKJFD";
		Random r= new Random();
		StringBuilder sb = new StringBuilder();
		for( int i = 0 ; i < 8 ; i ++) {
			int idx = r.nextInt(token.length());
			sb.append(token.charAt(idx));
		}
		connector.tryLogin(sb.toString());
		this.setTitle(connector.nickName);
	}
	
	void processSendMessage() {
		String msg = inputField.getText();
		// FIXME 검증 - 메세지 없는 경우
		connector.sendMessage(msg);
	}
	

	class ServerConnector extends Thread {
		boolean running ;
		Socket socket;
		BufferedReader br = null;
		PrintWriter out;
		String nickName;
		
		public ServerConnector(Socket s) {
			this.socket = s;
			initStream();
		}
		
		public void sendLogout() {
			String msg = "LOGOUT";
			out.println(msg);
			out.flush();
			Log.info(msg);
		}

		public void tryLogin(String nickName) {
			this.nickName = nickName;
			this.setName("T-" + nicknames);
			out.println("LOGIN|" + nickName);
			out.flush();
		}
		
		public void sendMessage(String text) {
			// MSG|dlalsdkfjas;dlfkjasd;lk
			String msg = "MSG|" + text;
			out.println(msg);
			out.flush();
			Log.info(msg);
		}
		
		void initStream() {
			try {
				InputStream in = null ;
				in = socket.getInputStream();
				br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				
				OutputStream os = socket.getOutputStream();
				out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
				
			} catch (IOException e) {
				throw new RuntimeException("error getting inputstream from server: ", e);
			}			
		}
		
		@Override
		public void run() {
			running = true;
			
			while ( running) {
				// 
				String input = null;
				try {
					Log.info("ready....");
					input = br.readLine(); // MSG|"jack"|"DOAKSDLFKE;LAKDSFJD;SALK"
					Log.info(input);
					String [] params = input.split("\\|");
					if ( "MSG".equals(params[0].trim()) ) {
						// 새로운 메세지 MSG|SENDER|DKDKDKDA;LKSFJD;L ASD;LFKJASD;L
						String sender = params[1].trim(); // Jack, tom
						String message = params[2].trim();
						chatArea.append(String.format("[%s ] %s\n", sender,message));
					} else if ( "JOINED".equals(params[0].trim())) {
						// JOINED|kim
						// 새로운 참여자
						String newJoiner = params[1].trim();
						nicknames.addElement(newJoiner);
						chatArea.append(String.format("%s입장\n", newJoiner));
					} else if ( "CHATTER-EXIT".equals(params[0].trim())) {
						// 누가 나갔음
						String outChatter = params[1].trim();
						nicknames.removeElement(outChatter);
						
					} else if ( "CHATTERS".equals(params[0].trim())) {
						// CHATTERS|jack,tom,ss
						String [] nicks = params[1].trim().split(",");
						for (String nick : nicks) {
							nicknames.addElement(nick);
						}
					}
					else {
						String error = String.format("알 수 없는 명령어 : %s ( 전문 : %s)", params[0].trim(), input);
						throw new RuntimeException(error);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					
				}
				
			}
		}
	}
	
	public static void main(String[] args) {
		new ChatClient().setVisible(true);
	}
}
