package chat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	public static void info(String msg ) {
		String m = String.format(
				"[%s][%s]%s\n", 
				Thread.currentThread().getName(), 
				curTime(), 
				msg);
		System.out.println(m);
	}

	static String curTime() {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		return df.format(new Date());
	}
}
