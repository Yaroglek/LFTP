import java.io.IOException;

import client.Client;

public class LFTP {
	public static void main(String[] args) {
		try {
			new Client(args);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}