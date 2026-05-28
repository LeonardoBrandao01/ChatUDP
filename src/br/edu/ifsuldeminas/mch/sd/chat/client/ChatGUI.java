package br.edu.ifsuldeminas.mch.sd.chat.client;

import br.edu.ifsuldeminas.mch.sd.chat.ChatException;
import br.edu.ifsuldeminas.mch.sd.chat.ChatFactory;
import br.edu.ifsuldeminas.mch.sd.chat.MessageContainer;
import br.edu.ifsuldeminas.mch.sd.chat.Sender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatGUI extends JFrame implements MessageContainer {

    private JTextField fldPortaLocal;
    private JTextField fldIPRemoto;
    private JTextField fldPortaRemota;
    private JTextField fldUsername;
    private JRadioButton radUDP;
    private JRadioButton radTCP;
    private JButton btnIniciar;
    private JLabel lblStatus;
    private JTextArea txtLog;
    private JTextField fldMensagem;
    private JButton btnEnviar;

    private Sender sender = null;
    private boolean connected = false;
    private String username = "Usuário";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatGUI() {
        super("Chat UDP / TCP");
        buildUI();
        wireListeners();
    }

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 600);
        setMinimumSize(new Dimension(450, 500));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        root.add(buildConfigPanel(), BorderLayout.NORTH);
        root.add(buildLogPanel(), BorderLayout.CENTER);
        root.add(buildInputPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Configurações de Conexão",
                TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 6, 4, 6);
        g.weightx = 1.0;

        g.gridy = 0; g.gridx = 0; g.gridwidth = 1;
        panel.add(new JLabel("Porta Local:"), g);
        g.gridx = 1;
        fldPortaLocal = new JTextField("9001");
        panel.add(fldPortaLocal, g);

        g.gridx = 2;
        panel.add(new JLabel("IP Remoto:"), g);
        g.gridx = 3;
        fldIPRemoto = new JTextField("localhost");
        panel.add(fldIPRemoto, g);

        g.gridy = 1; g.gridx = 0;
        panel.add(new JLabel("Porta Remota:"), g);
        g.gridx = 1;
        fldPortaRemota = new JTextField("9002");
        panel.add(fldPortaRemota, g);

        g.gridx = 2;
        panel.add(new JLabel("Seu Nome:"), g);
        g.gridx = 3;
        fldUsername = new JTextField("Usuário");
        panel.add(fldUsername, g);

        g.gridy = 2; g.gridx = 0;
        panel.add(new JLabel("Protocolo:"), g);

        g.gridx = 1; g.gridwidth = 3;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radUDP = new JRadioButton("UDP", true);
        radTCP = new JRadioButton("TCP", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(radUDP);
        bg.add(radTCP);
        radioPanel.add(radUDP);
        radioPanel.add(radTCP);
        panel.add(radioPanel, g);

        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        lblStatus = new JLabel("● Desconectado");
        lblStatus.setForeground(Color.RED);
        panel.add(lblStatus, g);

        g.gridx = 2; g.gridwidth = 2;
        btnIniciar = new JButton("Iniciar Chat");
        panel.add(btnIniciar, g);

        return panel;
    }

    private JScrollPane buildLogPanel() {
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtLog.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Mensagens",
                TitledBorder.LEFT, TitledBorder.TOP));
        return scroll;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        fldMensagem = new JTextField();
        fldMensagem.setEnabled(false);
        fldMensagem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        btnEnviar = new JButton("Enviar");
        btnEnviar.setEnabled(false);

        panel.add(fldMensagem, BorderLayout.CENTER);
        panel.add(btnEnviar, BorderLayout.EAST);
        return panel;
    }

    private void wireListeners() {
        btnIniciar.addActionListener(e -> handleConnect());
        ActionListener enviar = e -> handleSend();
        btnEnviar.addActionListener(enviar);
        fldMensagem.addActionListener(enviar);
    }

    private void handleConnect() {
        if (connected) return;

        int localPort;
        try {
            localPort = Integer.parseInt(fldPortaLocal.getText().trim());
        } catch (NumberFormatException ex) {
            showErr("Porta Local inválida.");
            return;
        }

        int remotePort;
        try {
            remotePort = Integer.parseInt(fldPortaRemota.getText().trim());
        } catch (NumberFormatException ex) {
            showErr("Porta Remota inválida.");
            return;
        }

        String remoteIP = fldIPRemoto.getText().trim();
        if (remoteIP.isEmpty()) {
            showErr("IP Remoto não pode estar vazio.");
            return;
        }

        username = fldUsername.getText().trim();
        if (username.isEmpty()) {
            showErr("Seu Nome não pode estar vazio.");
            return;
        }

        boolean isTCP = radTCP.isSelected();

        setFieldsEnabled(false);
        btnIniciar.setEnabled(false);
        lblStatus.setText("● Conectando...");
        lblStatus.setForeground(Color.ORANGE);

        // A tentativa de conexão do TCP bloqueia infinitamente até achar um peer,
        // logo precisamos de uma thread separada para não congelar a GUI.
        new Thread(() -> {
            try {
                Sender s = ChatFactory.build(isTCP, remoteIP, remotePort, localPort, this);
                SwingUtilities.invokeLater(() -> onConnected(s, isTCP ? "TCP" : "UDP"));
            } catch (ChatException ex) {
                SwingUtilities.invokeLater(() -> {
                    showErr("Erro ao iniciar chat: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    setFieldsEnabled(true);
                    btnIniciar.setEnabled(true);
                    lblStatus.setText("● Desconectado");
                    lblStatus.setForeground(Color.RED);
                });
            }
        }).start();
    }

    private void onConnected(Sender s, String protocolo) {
        this.sender = s;
        this.connected = true;

        fldMensagem.setEnabled(true);
        btnEnviar.setEnabled(true);
        fldMensagem.requestFocusInWindow();

        lblStatus.setText("● Conectado [" + protocolo + "]");
        lblStatus.setForeground(new Color(0, 130, 0));

        logSystem("Chat iniciado em " + protocolo);
    }

    private void setFieldsEnabled(boolean enabled) {
        fldPortaLocal.setEnabled(enabled);
        fldIPRemoto.setEnabled(enabled);
        fldPortaRemota.setEnabled(enabled);
        fldUsername.setEnabled(enabled);
        radUDP.setEnabled(enabled);
        radTCP.setEnabled(enabled);
    }

    private void handleSend() {
        if (!connected || sender == null) return;
        String texto = fldMensagem.getText().trim();
        if (texto.isEmpty()) return;

        // Formata como a UDP enviava e a newMessage processava ou apenas envia com nome.
        String payload = username + ": " + texto;
        try {
            sender.send(payload);
            log("[" + now() + "] Eu: " + texto);
            fldMensagem.setText("");
        } catch (ChatException ex) {
            showErr("Erro ao enviar: " + ex.getMessage());
        }
    }

    @Override
    public void newMessage(String message) {
        if (message == null) return;
        String msg = message.trim().replace("\u0000", "");
        if (msg.isEmpty()) return;

        SwingUtilities.invokeLater(() -> log("[" + now() + "] " + msg));
    }

    private void log(String linha) {
        txtLog.append(linha + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void logSystem(String msg) {
        log("--- " + msg + " ---");
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
