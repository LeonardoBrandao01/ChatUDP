package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.ChatException;
import br.edu.ifsuldeminas.sd.chat.MessageContainer;
import br.edu.ifsuldeminas.sd.chat.Receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP implementation of the {@link Receiver} interface.
 *
 * <p>Supports two modes:</p>
 * <ul>
 *   <li><b>Server mode</b>: Opens a {@link ServerSocket} on a given local port and
 *       waits for one incoming client connection, then starts reading.</li>
 *   <li><b>Client mode</b>: Connects to a remote host/port and starts reading.</li>
 * </ul>
 *
 * <p>After construction, a daemon thread is started automatically. Incoming lines
 * are dispatched to the provided {@link MessageContainer}.</p>
 */
public class TCPReceiver implements Receiver {

    /** Callback that receives lines delivered by this receiver. */
    private final MessageContainer container;

    /**
     * The socket used for the active peer connection.
     * In server mode this is the socket returned by {@link ServerSocket#accept()}.
     * In client mode this is the outgoing socket.
     */
    private Socket peerSocket;

    /** Whether the reading loop is already running. */
    private boolean isRunning = false;

    // -------------------------------------------------------------------------
    // Factory helpers – keeps construction simple for callers
    // -------------------------------------------------------------------------

    /**
     * Creates a TCPReceiver in <em>server</em> mode.
     * The receiver will open a {@link ServerSocket} on {@code localPort},
     * block until one client connects, and then start the reading loop.
     *
     * <p>The returned {@link TCPSender} shares the same peer socket, so both
     * directions of the conversation use the same TCP connection.</p>
     *
     * @param localPort port to listen on (must be > 1024)
     * @param container callback that receives incoming messages
     * @return a {@link TCPSender} bound to the accepted peer connection
     * @throws ChatException if the server socket cannot be created or the accept fails
     */
    public static TCPSender createServerMode(int localPort, MessageContainer container)
            throws ChatException {
        try {
            ServerSocket serverSocket = new ServerSocket(localPort);
            container.newMessage(String.format("[ TCP ] Aguardando conexão na porta %d…", localPort));
            Socket peer = serverSocket.accept();
            serverSocket.close(); // Only one peer is accepted
            container.newMessage(String.format("[ TCP ] Cliente conectado: %s",
                    peer.getInetAddress().getHostAddress()));

            TCPReceiver receiver = new TCPReceiver(peer, container);
            new Thread(receiver).start();

            return new TCPSender(peer);
        } catch (IOException e) {
            throw new ChatException("Falha ao iniciar o servidor TCP.", e);
        }
    }

    /**
     * Creates a TCPReceiver in <em>client</em> mode.
     * Connects to {@code remoteHost}:{@code remotePort} and starts the reading loop.
     *
     * @param remoteHost remote host name or IP address
     * @param remotePort remote port (must be > 0)
     * @param container  callback that receives incoming messages
     * @return a {@link TCPSender} bound to the remote host
     * @throws ChatException if the connection cannot be established
     */
    public static TCPSender createClientMode(String remoteHost, int remotePort,
                                             MessageContainer container) throws ChatException {
        try {
            Socket peer = new Socket(remoteHost, remotePort);
            container.newMessage(String.format("[ TCP ] Conectado a %s:%d", remoteHost, remotePort));

            TCPReceiver receiver = new TCPReceiver(peer, container);
            new Thread(receiver).start();

            return new TCPSender(peer);
        } catch (IOException e) {
            throw new ChatException(
                    String.format("Não foi possível conectar a %s:%d.", remoteHost, remotePort), e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal constructor
    // -------------------------------------------------------------------------

    private TCPReceiver(Socket peerSocket, MessageContainer container) {
        if (peerSocket == null) throw new IllegalArgumentException("Socket não pode ser nulo.");
        if (container == null)  throw new IllegalArgumentException("Container não pode ser nulo.");
        this.peerSocket  = peerSocket;
        this.container   = container;
    }

    // -------------------------------------------------------------------------
    // Runnable / reading loop
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        if (isRunning) return;
        isRunning = true;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(peerSocket.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    container.newMessage(line);
                }
            }
        } catch (IOException e) {
            container.newMessage("[ TCP ] Conexão encerrada com o peer.");
        } finally {
            isRunning = false;
        }
    }
}
