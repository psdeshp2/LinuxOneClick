/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ncsu.vcl.OneClick.App.ui;

import edu.ncsu.vcl.OneClick.reservation.IRequestListener;
import edu.ncsu.vcl.OneClick.reservation.Request;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author hp
 */
public class frmRequest extends javax.swing.JFrame implements IRequestListener {
	Request request;
    
    /**
     * Creates new form frmRequest
     */
    public frmRequest() {
        initComponents();
		btnClose.setVisible(false);
    }
    
    private void startReservation() {
		request = null;
		request = new Request(this);
		request.start();
		progressBar.setVisible(true);
		btnClose.setVisible(false);
	}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        lblStatus = new javax.swing.JTextArea();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("One-Click Application");
        setLocationByPlatform(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        progressBar.setIndeterminate(true);

        jScrollPane1.setBorder(null);

        lblStatus.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        lblStatus.setColumns(20);
        lblStatus.setEditable(false);
        lblStatus.setFont(progressBar.getFont());
        lblStatus.setLineWrap(true);
        lblStatus.setRows(1);
        lblStatus.setWrapStyleWord(true);
        jScrollPane1.setViewportView(lblStatus);

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progressBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(150, 150, 150)
                        .addComponent(btnClose)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addComponent(btnClose)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
		startReservation();
	}//GEN-LAST:event_formWindowOpened

	private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
		System.exit(0);
	}//GEN-LAST:event_btnCloseActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea lblStatus;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables

    @Override
    public void reservationStateChanged(String newState, String message) {
        lblStatus.setText(message);
    }

    @Override
    public void connectionParametersReceived(String osType, String serverIP, String user, String password, boolean autologin) {
		progressBar.setVisible(false);
		if(!autologin) {
			lblStatus.setText("Your '" + osType + "' reservation is ready, but the One-Click is not configured to automatically connect. Please connect manually.\n\nHost: " + serverIP + "\nUser: " + user+ "\nPassword: " + password);
			return;
		}
		lblStatus.setText("Ready to connect. Automatically connecting to reservation.");	
		try {
			if(osType.equals("linux") || osType.equals("unix")) {
				loadSSHClient(serverIP, user, password);
			}
			else if (osType.equals("windows")) {
				loadRDPClient(serverIP, user, password);
			}
			else {
				lblStatus.setText("OS type '" + osType + "' is not supported for automatic login. Please attempt to manually connect.\n\nHost: " + serverIP + "\nUser: " + user+ "\nPassword: " + password);
			}
		}
		catch(Exception e) {
			lblStatus.setText("Unable to connect automatically to the '" + osType + "' reservation. Please attempt to manually connect.\n\nHost: " + serverIP + "\nUser: " + user+ "\nPassword: " + password);
		}
		btnClose.setVisible(true);
    }

    @Override
    public void errorReported(String errorCode, String errorMessage) {
		if(errorCode.contains("Access denied")) {
			lblStatus.setText("Stored credentials are invalid");
			progressBar.setVisible(false);
			dlgLogin login = new dlgLogin(null, true);
			login.setVisible(true);
			if(!login.wasSaved()) {
				System.exit(0);
			}
			login = null;
			startReservation();
			return;
		}
		lblStatus.setText(errorMessage);
		progressBar.setVisible(false);
		btnClose.setVisible(true);
    }
	
	
    private void loadRDPClient(String host, String user, String password) throws IOException {
		try {
			Process p = Runtime.getRuntime().exec("which rdesktop");
			InputStream lsOut = p.getInputStream();
			InputStreamReader r = new InputStreamReader(lsOut);
			BufferedReader in = new BufferedReader(r);
			
			String command = null;
			command = in.readLine();
			in.close();
			r.close();
			lsOut.close();
			if(command==null || command.equals("")) {
				throw new IOException("Unable to find rdesktop");
			}
                    
			ProcessBuilder pb = new ProcessBuilder(command, host, "-u", user, "-p", password);
			pb.start();
			
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(this, "This reservation requires rdesktop to automatically connect, but it was not found on your system.\n\nPlease visit http://www.rdesktop.org/ for information on how to install it.", "Error", JOptionPane.ERROR_MESSAGE);
			throw e;
		}
    }

    private void loadSSHClient(String host, String user, String password) throws IOException {
		try {
			Process p = Runtime.getRuntime().exec("which expect");
			InputStream lsOut = p.getInputStream();
			InputStreamReader r = new InputStreamReader(lsOut);
			BufferedReader in = new BufferedReader(r);
			
			String ret = null;
			ret = in.readLine();
			in.close();
			r.close();
			lsOut.close();
			if (ret==null || ret.equals("")) {
			    throw new IOException("Unable to find rdesktop");
			}

			String command = "xterm";
			String ssh = "ssh";
			String expect = "expect";
			String query = user+"@"+host;
			ProcessBuilder pb = new ProcessBuilder(command, "-e", expect, "-c","spawn ssh "+query+";expect \"*?assword:*\";send -- \"yes\r\"; send -- \""+password+"\r\";interact;expect eof");
			try {
			    //pb.redirectInput().
			    Thread.sleep(3000);
			} catch (InterruptedException ex) {
			}
			p = pb.start();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "This reservation requires 'expect' to automatically connect, but it was not found on your system.\n\nPlease run sudo apt-get install expect", "Error", JOptionPane.ERROR_MESSAGE);
			throw ex;
		}

    }
}
