package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.ChatException;
import br.edu.ifsuldeminas.sd.chat.Sender;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * TCP implementation of the {@link Sender} interface.
 * Sends messages over a persistent TCP connection using a {@link PrintWriter}.
 */
public class TCPSender implements Sender {

    private final Socket socket;
    private final PrintWriter writer;

    /**
     * Creates a TCPSender from an already-established {@link Socket}.
     * The socket must be connected before passing it here.
     *
     * @param socket an open, connected TCP socket
     * @throws ChatException if the output stream cannot be obtained
     */
    public TCPSender(Socket socket) throws ChatException {
        if (socket == null || socket.isClosed()) {
            throw new ChatException("Socket TCP inválido ou fechado.",
                    new IllegalArgumentException("Socket nulo ou fechado."));
        }
        this.socket = socket;
        try {
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new ChatException("Não foi possível obter o stream de saída do socket TCP.", e);
        }
    }

    /**
     * Sends a message over the TCP connection.
     *
     * @param message the message string to send
     * @throws ChatException if the socket is closed or the writer has an error
     */
    @Override
    public void send(String message) throws ChatException {
        if (socket.isClosed() || writer.checkError()) {
            throw new ChatException("Conexão TCP encerrada. Não foi possível enviar a mensagem.",
                    new IOException("Socket fechado ou stream com erro."));
        }
        writer.println(message);
    }

    /**
     * Closes the underlying socket and writer.
     */
    public void close() {
        writer.close();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
