/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ncsu.vcl.OneClick.App;

import edu.ncsu.vcl.OneClick.App.ui.dlgLogin;
import edu.ncsu.vcl.OneClick.App.ui.frmRequest;
import edu.ncsu.vcl.OneClick.security.CredentialStore;
import edu.ncsu.vcl.OneClick.security.CredentialStore.Credentials;

/**
 *
 * @author ignacioxd
 */
public class Main {
    public static void main(String[] args){
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
		}
		
		CredentialStore cs = new CredentialStore();
		if(args.length == 1 && args[0].equals("-clear")) {
			cs.clearCredentials();
			return;
		}
		
        Credentials creds = cs.getCredentials();
        if(creds == null || creds.getPassword().equals("")) {
            dlgLogin login = new dlgLogin(null, true);
			login.setVisible(true);
			if(!login.wasSaved()) {
				System.exit(0);
			}
			login = null;
        }
        /* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new frmRequest().setVisible(true);
			}
		});
    }
    
}
