
// https://github.com/SilverJun/SimpleChat

import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap<String, PrintWriter> hm = new HashMap<String, PrintWriter>();							// hashmap contains id and 
			while(true){
				Socket sock = server.accept();		// wait for new client.
				ChatThread chatthread = new ChatThread(sock, hm);		// start thread
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private static String[] badWordList = {"시발", "씨발","새끼", "존나", "씹", "지랄", "좆", "꺼저"};	// static 배열을 만들어서 비속어 필터링때 사용하자.

	private Socket sock;
	private String id;
	private BufferedReader br;		// recv
	private HashMap hm;
	private boolean initFlag = false;
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			String tempDate = getCurTimeString();
			System.out.println(tempDate+"[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			String tempDate = getCurTimeString();
			System.out.println(tempDate+ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if (isBadWord(line))			// 만약 문장에 비속어가 있어면 체크하고 없애주기.
				{
					String tempDate = getCurTimeString();
					synchronized(hm){
						PrintWriter pw = (PrintWriter)hm.get(id);
						pw.println(tempDate+"경고: 비속어를 사용해 서버에 의해 필터링 되었습니다.");		// 그 사람에게 경고.
						pw.flush();
					}
					continue;
				}
				if(line.equals("/quit"))
					break;
				else if(line.indexOf("/to ") == 0){
					sendmsg(line);
				}
				else if (line.equals("/userlist")) {
					send_userlist();
				}
				else
					broadcast(id + " : " + line);
			}
		}catch(Exception ex){
			String tempDate = getCurTimeString();
			System.out.println(tempDate+ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		String tempDate = getCurTimeString();

		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(tempDate + id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg

	/**
	 * @brief broadcast message
	 * @param msg
	 */
	public void broadcast(String msg){
		synchronized(hm){
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			PrintWriter self = (PrintWriter)hm.get(id);		// 자신의 PrintWriter를 받아와서

			// clock add.

			while(iter.hasNext()){
				PrintWriter pw = (PrintWriter)iter.next();
				if (pw.equals(self))						// 같은지 비교.
					continue;								// 같으면 보내지 않고 다음으로 넘어가기.
					
				String tempDate = getCurTimeString();
				pw.println(tempDate+msg);
				pw.flush();
			}
		}
	} // broadcast

	/**
	 * userlist를 사용자에게 보내준다.
	 * hm에서 키들을 싹 가져와서 id의 printwriter에 보내주면 된다.
	 */
	public void send_userlist() {
		synchronized(hm) {
			PrintWriter pw = (PrintWriter)hm.get(id);
			pw.println("----start userlist----");
			Collection collection = hm.keySet();			// 키들의 집합을 가져옴.
			Iterator iter = collection.iterator();
			while(iter.hasNext()){
				pw.println((String)iter.next());			// 키들을 전부 전송.
				pw.flush();
			}
			pw.println("total: " + collection.size());		// 토탈 size까지 전송. 끝.
			pw.flush();
		}
	}
	/**
	 * 비속어가 들어있으면 리턴해주는 함수.
	 * @return 비속어 있으면 true, 없으면 false
	 */
	public boolean isBadWord(String line)
	{
		for(String word:badWordList)
		{
			if (line.contains(word))			// 단어가 있는지 확인
				return true;
		}
		return false;
	}

	public String getCurTimeString()
	{
		SimpleDateFormat sdFormat = new SimpleDateFormat("[HH:mm:ss]");
		Date nowDate = new Date();
		String tempDate = sdFormat.format(nowDate);
		tempDate += " ";
		return tempDate;
	}
}
