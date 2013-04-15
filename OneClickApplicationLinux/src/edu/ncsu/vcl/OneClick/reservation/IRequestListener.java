package edu.ncsu.vcl.OneClick.reservation;

public interface IRequestListener {
	public void reservationStateChanged(String newState, String message);
	public void connectionParametersReceived(String osType, String serverIP, String user, String password, boolean autologin,Request request);
	public void errorReported(String errorCode, String errorMessage);
}