package edu.ncsu.vcl.OneClick.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;


public class CredentialStore {
	String USER_FILE = System.getProperty("user.home") + "/.vclUser";
	String PASS_FILE = System.getProperty("user.home") + "/.vclPass";

	public CredentialStore() {
	}
	
	public Credentials getCredentials() {
		try {
			String user = readFile(new File(USER_FILE));
			String pass = readFile(new File(PASS_FILE));
			return new Credentials(user, pass);
		} catch (Exception e) {
		}
		return null;
		//return new Credentials("admin", "Adminuserpass123");
	}
	
	
	public void setCredentials(String user, String password) {
		try {
			clearCredentials();
			writeFile(new File(USER_FILE), user);
			writeFile(new File(PASS_FILE), password);
		}
		catch(Exception e) {
		}
		
	}
	
	private String readFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private void writeFile(File installation, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        out.write(content.getBytes());
        out.close();
    }
	
	public void clearCredentials() {
		new File(USER_FILE).delete();
		new File(PASS_FILE).delete();
	}
	
	public class Credentials {
		private String user;
		private String password;
		
		private Credentials(String user, String password) {
			this.user = user;
			this.password = password;
		}
		
		public String getPassword() {
			return password;
		}
		
		public String getUser() {
			return user;
			
		}
	}
}