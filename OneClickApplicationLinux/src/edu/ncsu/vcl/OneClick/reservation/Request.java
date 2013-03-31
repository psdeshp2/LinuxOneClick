package edu.ncsu.vcl.OneClick.reservation;

import edu.ncsu.vcl.OneClick.App.Main;
import java.io.InputStream;
import java.util.HashMap;


import edu.ncsu.vcl.OneClick.security.CredentialStore;
import edu.ncsu.vcl.OneClick.security.CredentialStore.Credentials;


import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;

    

public class Request extends Thread {
	XmlRpcClient apiClient;
	IRequestListener listener;
	
	
	public Request(IRequestListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		String vclURL = "";
		String oneClickId = "";
		//Read configuration file for vclURL and oneClickId
		/*final Properties props = new Properties();
		props.load(stream);
		vclURL = props.getProperty("vclURL");*/
		
		URL uri = null;
        try {
			final URL url = Main.class.getResource("config");
			if (url == null) {
				throw new Exception("Failed to locate resource: config" );
			}
			InputStream is = url.openStream();
        	byte[] data = new byte[1024];
        	int length = 0;
        	byte byteData = (byte)is.read();
			for(int i = 0; byteData != -1; i++) {
				data[i] = byteData;
				length++;
				byteData = (byte) is.read();
			}
			is.close();
			is = null;
			
			String[] lines = new String(data, 0, length).split("\n");
			for(int i = 0; i < lines.length; i++) {
				if(lines[i].equals(""))
					continue;
				String[] keyval = lines[i].split("=", 2);
				if(keyval[0].equals("vclURL")) {
					vclURL = keyval[1];
				}
				else if(keyval[0].equals("oneClickID")) {
					oneClickId = keyval[1];
				}
			}
			
			if(vclURL.equals("") || oneClickId.equals(""))
				throw new Exception();
			
			uri = new URL(vclURL);
        }
        catch(Exception e) {
			e.printStackTrace();
        	sendErrorMessage("10", "There was a problem with the configuration file. This One-Click instance is invalid.");
			return;
        }
        sendStateChange("configLoaded", "Configuration successfully loaded.");
        
        CredentialStore cs = new CredentialStore();
        Credentials creds = cs.getCredentials();
        
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("X-User", creds.getUser());
        headers.put("X-Pass", creds.getPassword());
        headers.put("X-OneClick", oneClickId);
        headers.put("X-APIVERSION", "2");
        
		
		
        //implement transport factory
		XmlRpcTransportFactory xmlRpcTransportFactory = new XmlRpcSunHttpTransportFactory(apiClient) {
			@Override
			public XmlRpcTransport getTransport() {
				return new XmlRpcSunHttpTransport(apiClient) {
					@Override
					protected void initHttpHeaders(XmlRpcRequest pRequest) throws XmlRpcClientException {
						try {
							super.initHttpHeaders(pRequest);

							Set keys = headers.keySet();
							for(Iterator iter = keys.iterator(); iter.hasNext();) {
							   String key = (String) iter.next();
							   setRequestHeader(key, headers.get(key));
							}
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
				};
			}
		};
		
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					@Override
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
						return true;
				}
			};

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		apiClient = new XmlRpcClient();
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(uri);
        config.setEnabledForExtensions(true);
        config.setContentLengthOptional(false);
		apiClient.setConfig(config);
        apiClient.setTransportFactory(xmlRpcTransportFactory);
        
		HashMap<?, ?> result;
		int duration = 0;
		int imageId = 0;
		int requestId = 0;
		String ostype =  "";
		boolean autologin = false;
		try {
			result = (HashMap<?, ?>)apiClient.execute("XMLRPCgetOneClickParams", new Object[]{ oneClickId });
			if(((String)result.get("status")).equals("success")) {
				imageId = (Integer)result.get("imageid");
				ostype  = (String)result.get("ostype");
				duration = (Integer)result.get("duration");
				autologin = ((Integer)result.get("autologin")) == 1;
				sendStateChange("oneclickreceived", "One-Click parameters were successfully retrieved. Requesting reservation.");
			}
			else {
				int errorcode = (Integer)result.get("errorcode");
				if (errorcode == 3)
					sendErrorMessage(result.get("errorcode")+"", "Access Denied");
				else if (errorcode == 4)
					sendErrorMessage(result.get("errorcode")+"", "This One-Click configuration does not exist or has been deleted on the server.");
				else if (errorcode == 5)
					sendErrorMessage(result.get("errorcode")+"", "The user does not have enough privileges to use One-Click functionality.");
				else if(errorcode == 6)
					sendErrorMessage(result.get("errorcode")+"", "This One-Click configuration belongs to a different user. Please create one-click and try again.");
				else
					sendErrorMessage(result.get("errorcode")+"", "Unable to retrieve One-Click parameters.");
				return;
			}

		} catch (final XmlRpcException e) {
			sendErrorMessage(e.getMessage(), e.getStackTrace().toString());
			e.printStackTrace();
			return;
		} catch (final Exception e) {
			sendErrorMessage(e.getMessage(), "Unable to retrieve One-Click parameters.");
			e.printStackTrace();
			return;
		}
		try {
			result = (HashMap<?, ?>)apiClient.execute("XMLRPCaddRequest", new Object[]{imageId+"", "now", duration+"", oneClickId});
			if(((String)result.get("status")).equals("success")) {
				requestId = Integer.parseInt((String)result.get("requestid"));
                           //     sendStateChange("oneclickreceived", "Inside success, Request id = " + requestId);
			}
			else if(((String)result.get("status")).equals("notavailable")) {
                //             sendStateChange("oneclickreceived", "Dint go inside success");
				sendErrorMessage("0", "No computers were available for the request.");
				return;
			}
			else {
                //             sendStateChange("oneclickreceived", "Dint go inside success");
				sendErrorMessage((String)result.get("errorcode"), "Reservation request failed.");
				return;
			}
			sendStateChange("reserved", "Reservation request successfully submitted.");
		} catch (final XmlRpcException e) {
			sendErrorMessage(e.getMessage(), "Reservation request could not be submitted.");
			e.printStackTrace();
			return;
		}
		try {	
			
			boolean ready = false;
			while(!ready) {
				result = (HashMap<?, ?>)apiClient.execute("XMLRPCgetRequestStatus", new Object[]{requestId+""});
				String status = (String)result.get("status");
				if(status.equals("error")) {
					sendErrorMessage((String)result.get("errorcode"), "Problem getting the status of the reservation.");
					return;
				}
				else if(status.equals("failed")) {
					sendStateChange(status, "The VCL reservation failed.");
					return;
				}
				else if(status.equals("timedout")) {
					sendStateChange(status, "The VCL reservation timed out.");
					return;
				}
				else if(status.equals("loading")) {
					sendStateChange(status, "VCL resource is being prepared; Est. time remaining: " + (result.get("time")) + " minute(s).");
					//stateHandler(newstate='updatewait');
				}
				else if(status.equals("future")) {
					sendStateChange(status, "The start time for this reservation has not been reached yet.");
				}
				else if(status.equals("ready")) {
					sendStateChange(status, "The reservation is ready. Obtaining connection parameters.");
					ready = true;
				}
				Thread.sleep(5000);
				
			}
		} catch (final XmlRpcException e) {
			sendErrorMessage(e.getMessage(), "Problem getting the status of the reservation.");
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		String publicIP = "";
		try{
			result = (HashMap<?, ?>)apiClient.execute("XMLRPCgetIP", new Object[0]);
			if (((String)result.get("status")).equals("success")){
				publicIP = (String)result.get("ip");
			}
			else{
				throw new XmlRpcException("");
			}
		}
		catch(XmlRpcException e){
			sendErrorMessage("0", "Unable to get external IP address. Please go to the VCL website to obtain the connection parameters.");
		}
		try {	
			result = (HashMap<?, ?>)apiClient.execute("XMLRPCgetRequestConnectData", new Object[]{requestId+"", publicIP});
			if( ((String)result.get("status")).equals("ready") ) {
				sendConnectionParameters(ostype, (String)result.get("serverIP"), (String)result.get("user"), (String)result.get("password"), autologin);
			}
			else if(((String)result.get("status")).equals("notready")) {
				sendErrorMessage("0", "The request is not yet ready.");
			}
			else {
				sendErrorMessage((result.get("errorcode"))+"", "Unable to get connection parameters. You can find these parameters in the VCL website.");
			}
			
		} catch (final XmlRpcException e) {
			sendErrorMessage(e.getMessage(), "Unable to get connection parameters. You can find these parameters in the VCL website.");
			e.printStackTrace();
			return;
		}
	}
	
	private String getLocalIP() {
        try {
            for (Enumeration< NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
                NetworkInterface cur = interfaces.nextElement();

                if (cur.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress addr : cur.getInterfaceAddresses()) {
                    InetAddress inet_addr = addr.getAddress();

                    if (!(inet_addr instanceof Inet4Address)) {
                        continue;
                    }
                    return inet_addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
	
	private String getPublicIP(){
		String result = "127.0.0.1";
		try {
			java.net.URL URL = new java.net.URL("http://www.whatismyip.org/");
			java.net.HttpURLConnection Conn = (HttpURLConnection)URL.openConnection();
			java.io.InputStream InStream = Conn.getInputStream();
			java.io.InputStreamReader Isr = new java.io.InputStreamReader(InStream);
			java.io.BufferedReader Br = new java.io.BufferedReader(Isr);
			result = Br.readLine();
			Br.close();
			Isr.close();
			InStream.close();
			Conn.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	public void sendErrorMessage(final String code, final String message) {
		listener.errorReported(code, message);
	}
	
	public void sendStateChange(final String newState, final String message) {
		listener.reservationStateChanged(newState, message);
	}
	
	public void sendConnectionParameters(final String osType, final String serverIP, final String user, final String password, final boolean autologin) {
		listener.connectionParametersReceived(osType, serverIP, user, password, autologin);
	}
	
}
