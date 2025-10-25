package controlador;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public interface InterfazCB extends Remote {

    public void notificarNuevaConexion(String id, InterfazCB amigo) throws RemoteException;

    public void notificarDesconexion(String id, InterfazCB amigo) throws RemoteException;

    public void enviar(String remitente, String mensaje) throws RemoteException;

    public void recibir(String remitente, String mensaje) throws RemoteException;

    public void notificarNuevaSolicitud(String envia) throws RemoteException;

    public void recibirMensajePrivado(String remitente, String mensaje) throws RemoteException;

    public void recibirSolicitud(String mensaje) throws RemoteException;

    public void recibirSolicitudes(List<String> solicitudesPendientes) throws RemoteException;

    public void nuevoAmigo(String id, InterfazCB amigo) throws RemoteException;
    
    public void errorAmigo() throws RemoteException;

    public void listaAmigosEnLinea(HashMap<String, InterfazCB> amigosEnLinea) throws RemoteException;
}
