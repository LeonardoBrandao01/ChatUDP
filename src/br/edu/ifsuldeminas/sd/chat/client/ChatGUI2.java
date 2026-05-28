package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.ChatException;
import br.edu.ifsuldeminas.sd.chat.ChatFactory;
import br.edu.ifsuldeminas.sd.chat.MessageContainer;
import br.edu.ifsuldeminas.sd.chat.Sender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatGUI2 extends JFrame implements MessageContainer {
    // UI Components for Connection Setup
    private JTextField txtPortaLocal;
    private JTextField txtIPRemoto;
    private JTextField txtPortaRemota;
    private JTextField txtUsername;
    private JButton btnConnect;
    private JLabel lblStatus;

    // UI Components for Chat Log and Message Input
    private JTextArea txtChatLog;
    private JTextField txtMessageInput;
    private JButton btnEnviar;

    // Chat API variables
    private Sender chatSender = null;
    private boolean isConnected = false;

    public ChatGUI2() {
        super("Chat UDP - Interface Gráfica");
        setupUI();
    }

    private void setupUI() {
        // Use system look and feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore and fall back to default
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 600);
        setMinimumSize(new Dimension(450, 500));
        setLocationRelativeTo(null); // Center on screen
        setLayout(new BorderLayout(10, 10));

        // Root panel with padding
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(rootPanel);

        // --- TOP PANEL: Connection Settings ---
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Configurações de Conexão",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(44, 62, 80)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        // Row 0: Local Port & Username
        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Porta Local:"), gbc);
        gbc.gridx = 1;
        txtPortaLocal = new JTextField("9002");
        connectionPanel.add(txtPortaLocal, gbc);

        gbc.gridx = 2;
        connectionPanel.add(new JLabel("Seu Nome:"), gbc);
        gbc.gridx = 3;
        txtUsername = new JTextField("Usuario");
        connectionPanel.add(txtUsername, gbc);

        // Row 1: Remote IP & Remote Port
        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("IP Remoto:"), gbc);
        gbc.gridx = 1;
        txtIPRemoto = new JTextField("localhost");
        connectionPanel.add(txtIPRemoto, gbc);

        gbc.gridx = 2;
        connectionPanel.add(new JLabel("Porta Remota:"), gbc);
        gbc.gridx = 3;
        txtPortaRemota = new JTextField("9001");
        connectionPanel.add(txtPortaRemota, gbc);

        // Row 2: Status & Connect Button
        gbc.gridx = 0; gbc.gridy = 2;
        lblStatus = new JLabel("🔴 Desconectado");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        connectionPanel.add(lblStatus, gbc);

        gbc.gridx = 1; gbc.gridwidth = 3;
        btnConnect = new JButton("Iniciar Chat");
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConnect.setBackground(new Color(46, 204, 113));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        connectionPanel.add(btnConnect, gbc);

        rootPanel.add(connectionPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: Chat Log ---
        txtChatLog = new JTextArea();
        txtChatLog.setEditable(false);
        txtChatLog.setLineWrap(true);
        txtChatLog.setWrapStyleWord(true);
        txtChatLog.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtChatLog.setBackground(new Color(250, 250, 250));
        txtChatLog.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(txtChatLog);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                new EmptyBorder(0, 0, 0, 0)));
        rootPanel.add(scrollPane, BorderLayout.CENTER);

        // --- BOTTOM PANEL: Message Input ---
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));

        txtMessageInput = new JTextField();
        txtMessageInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMessageInput.setEnabled(false); // Disabled until connected

        btnEnviar = new JButton("Enviar");
        btnEnviar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEnviar.setEnabled(false); // Disabled until connected
        btnEnviar.setBackground(new Color(46, 204, 113));
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setFocusPainted(false);

        inputPanel.add(txtMessageInput, BorderLayout.CENTER);
        inputPanel.add(btnEnviar, BorderLayout.EAST);

        rootPanel.add(inputPanel, BorderLayout.SOUTH);

        // --- LISTENERS ---
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleConnection();
            }
        });

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };

        btnEnviar.addActionListener(sendAction);
        txtMessageInput.addActionListener(sendAction); // Sends message when Enter is pressed
    }

    private void handleConnection() {
        if (isConnected) return;

        // Validate local port
        int localPort;
        try {
            localPort = Integer.parseInt(txtPortaLocal.getText().trim());
            if (localPort <= 1024 || localPort > 65535) {
                showError("Porta Local inválida. Escolha uma porta entre 1025 e 65535 (não reservada).");
                return;
            }
        } catch (NumberFormatException e) {
            showError("A Porta Local deve ser um número inteiro válido.");
            return;
        }

        // Validate remote port
        int remotePort;
        try {
            remotePort = Integer.parseInt(txtPortaRemota.getText().trim());
            if (remotePort <= 0 || remotePort > 65535) {
                showError("Porta Remota inválida. Escolha uma porta entre 1 e 65535.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("A Porta Remota deve ser um número inteiro válido.");
            return;
        }

        // Validate IP
        String remoteIP = txtIPRemoto.getText().trim();
        if (remoteIP.isEmpty()) {
            showError("O IP Remoto não pode estar vazio.");
            return;
        }

        // Validate Username
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            showError("O Nome de Usuário não pode estar vazio.");
            return;
        }

        try {
            // Build the sender and receiver using ChatFactory
            chatSender = ChatFactory.build(remoteIP, remotePort, localPort, this);
            isConnected = true;

            // Update UI State
            lblStatus.setText("🟢 Conectado");
            lblStatus.setForeground(new Color(46, 204, 113));
            txtPortaLocal.setEnabled(false);
            txtIPRemoto.setEnabled(false);
            txtPortaRemota.setEnabled(false);
            txtUsername.setEnabled(false);
            btnConnect.setEnabled(false);
            btnConnect.setText("Chat Ativo");

            txtMessageInput.setEnabled(true);
            btnEnviar.setEnabled(true);
            txtMessageInput.requestFocusInWindow();

            txtChatLog.append("=== Chat iniciado com sucesso! ===\n");
            txtChatLog.append(String.format("Porta Local: %d | Destino: %s:%d\n", localPort, remoteIP, remotePort));
            txtChatLog.append("==================================\n\n");

        } catch (ChatException e) {
            String causeMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            showError("Falha ao iniciar o Chat UDP: " + causeMsg);
        } catch (IllegalArgumentException e) {
            showError("Erro de argumento: " + e.getMessage());
        }
    }

    private void sendMessage() {
        if (!isConnected || chatSender == null) return;

        String text = txtMessageInput.getText().trim();
        if (text.isEmpty()) return;

        String username = txtUsername.getText().trim();

        // Format message as text::de::username
        String messagePayload = String.format("%s%s%s", text, MessageContainer.FROM, username);

        try {
            chatSender.send(messagePayload);

            // Append sent message to local view
            txtChatLog.append(String.format("Eu (%s): %s\n", username, text));
            txtMessageInput.setText("");
            txtMessageInput.requestFocusInWindow();

            // Auto-scroll
            txtChatLog.setCaretPosition(txtChatLog.getDocument().getLength());

        } catch (ChatException e) {
            showError("Não foi possível enviar a mensagem: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // MessageContainer implementation
    @Override
    public void newMessage(String message) {
        if (message == null) return;

        // Trim null bytes from receiving buffer and whitespace
        message = message.trim();
        if (message.isEmpty()) return;

        String sender = "Desconhecido";
        String messageText = message;

        if (message.contains(MessageContainer.FROM)) {
            String[] parts = message.split(MessageContainer.FROM);
            if (parts.length >= 2) {
                messageText = parts[0];
                sender = parts[1];
            } else if (parts.length == 1) {
                messageText = parts[0];
            }
        }

        final String finalSender = sender;
        final String finalMessageText = messageText;

        // Update components safely on EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                txtChatLog.append(String.format("%s: %s\n", finalSender, finalMessageText));
                // Auto-scroll
                txtChatLog.setCaretPosition(txtChatLog.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatGUI2().setVisible(true);
            }
        });
    }
}
