/*
 * SMTPClientView.java
 */

package smtpclient;

import org.jdesktop.application.*;
import java.io.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
/**
 *
 * @author Predrag
 */
public class SMTPClientView extends FrameView {
     
     String smtp = "smtp.sbb.rs";
     int port = 25;
     
     public void sendMail(String mailFrom, String mailTo, String subject, String message, String attachmentPath, String attachmentName) {
            
         SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
         String dateString = format.format(new Date());
         
         String[] mailRcpt = new String[20];
         mailRcpt = mailTo.split(",");
         
         //Uzimamo extenziju attachmenta
         int position = attachmentName.lastIndexOf('.');
         String extension = attachmentName.substring(position+1);    
         
         String boundary = "DataSeparatorString";
         
         try {   
            // počninje do-while petlja kako bi omogućili više rcpt
            int i = 0;
            do {
                // ispisujemo osnovne konfiguracije i podatke
                System.out.println("Primalac: " + mailRcpt[i] + " od " + mailRcpt.length);
                System.out.println("Smtp Server: " + smtp + " | Broj porta: " + port);
 
                // vršimo konekciju na server
                Socket s = new Socket(smtp, port);
                
                // ulaz / izlaz
                BufferedReader inputStreamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter outputStreamToServer = new PrintWriter(s.getOutputStream(), true);                
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "8859_1"));

                // započinjemo komunikaciju
                System.out.println(inputStreamFromServer.readLine());
                // predstavljamo se serveru
                outputStreamToServer.println("HELO server.com");
                System.out.println(inputStreamFromServer.readLine());
                
                outputStreamToServer.println(String.format("MAIL FROM: <%s>", mailFrom));
                System.out.println(inputStreamFromServer.readLine());
                
                outputStreamToServer.println(String.format("RCPT TO: <%s>", mailRcpt[i]));
                System.out.println(inputStreamFromServer.readLine());
                
                outputStreamToServer.println("DATA");
                outputStreamToServer.println("MIME-Version: 1.0");
                outputStreamToServer.println("Subject: " + subject);
                outputStreamToServer.println(String.format("From: <%s>", mailFrom));
                outputStreamToServer.println(String.format("To: <%s>", mailRcpt[i]));                
                outputStreamToServer.println("Date: " + dateString);

                outputStreamToServer.println("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"");
                outputStreamToServer.println("\r\n--" + boundary);
                
                // body poruke
                outputStreamToServer.println("Content-Type: text/plain; charset=\"us-ascii\"\r\n");
                outputStreamToServer.println("Message: " + message);
                outputStreamToServer.println("\r\n--" + boundary);
                
                
                // saljemo poruku sa attachmentom i proveravamo ekstenziju prikacenog fajla
                if(!attachmentPath.equals("")) {
                    outputStreamToServer.println(ContentType.extensionCheck(extension, attachmentPath));
                    outputStreamToServer.println("Content-Disposition: attachment;filename=\""+attachmentName+"\"");
                    outputStreamToServer.println("Content-transfer-encoding: base64\r\n");
                    MIMEBase64.encode(attachmentPath, out);
                    outputStreamToServer.println("\r\n--" + boundary);
                    //outputStreamToServer.println("\r\n\r\n--" + boundary + "--\r\n");
                }
                
                outputStreamToServer.println(".");
                System.out.println(inputStreamFromServer.readLine());
                outputStreamToServer.println("QUIT");
                System.out.println(inputStreamFromServer.readLine());
                s.close();
                
                
                sentMessage.setText("Poruka je poslata na: " + mailRcpt[i]);
                i++;
            } while(i != mailRcpt.length);   
            
        } catch (IOException ex) {
            Logger.getLogger(SMTPClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    

    public SMTPClientView(SingleFrameApplication app) {
        super(app);

        
        initComponents();
        this.getFrame().setResizable(false);
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        
        
        
        /*********************************************************************************/        
        portNumber.setText("Port: " + port);
        smtpServer.setText("SMTP server: " + smtp);
        /*********************************************************************************/
        
        
        
        
        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SMTPClientApp.getApplication().getMainFrame();
            aboutBox = new SMTPClientAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SMTPClientApp.getApplication().show(aboutBox);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        attachmentNameJText = new javax.swing.JTextField();
        attachmentJText = new javax.swing.JTextField();
        attachmentJLabel = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        fromJLabel = new javax.swing.JLabel();
        fromJText = new javax.swing.JTextField();
        toJText = new javax.swing.JTextField();
        toJLabel = new javax.swing.JLabel();
        subjectJLabel = new javax.swing.JLabel();
        subjectJText = new javax.swing.JTextField();
        attachmentJLabel1 = new javax.swing.JLabel();
        jScrollPane = new javax.swing.JScrollPane();
        messagejTextArea = new javax.swing.JTextArea();
        clearButton = new javax.swing.JButton();
        sendButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        sentMessage = new javax.swing.JLabel();
        serverButton = new javax.swing.JButton();
        smtpServer = new javax.swing.JLabel();
        portNumber = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        jFileChooser = new javax.swing.JFileChooser();
        serverOptions = new javax.swing.JDialog();
        smtpServerText = new javax.swing.JTextField();
        portText = new javax.swing.JTextField();
        Submit = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(550, 550));

        attachmentNameJText.setEditable(false);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(smtpclient.SMTPClientApp.class).getContext().getResourceMap(SMTPClientView.class);
        attachmentNameJText.setFont(resourceMap.getFont("attachmentNameJText.font")); // NOI18N
        attachmentNameJText.setName("attachmentNameJText"); // NOI18N

        attachmentJText.setEditable(false);
        attachmentJText.setFont(resourceMap.getFont("attachmentJText.font")); // NOI18N
        attachmentJText.setName("attachmentJText"); // NOI18N

        attachmentJLabel.setFont(resourceMap.getFont("attachmentJLabel.font")); // NOI18N
        attachmentJLabel.setText(resourceMap.getString("attachmentJLabel.text")); // NOI18N
        attachmentJLabel.setName("attachmentJLabel"); // NOI18N

        browseButton.setText(resourceMap.getString("browseButton.text")); // NOI18N
        browseButton.setName("browseButton"); // NOI18N
        browseButton.setVerifyInputWhenFocusTarget(false);
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        fromJLabel.setFont(resourceMap.getFont("fromJLabel.font")); // NOI18N
        fromJLabel.setText(resourceMap.getString("fromJLabel.text")); // NOI18N
        fromJLabel.setName("fromJLabel"); // NOI18N

        fromJText.setFont(resourceMap.getFont("fromJText.font")); // NOI18N
        fromJText.setName("fromJText"); // NOI18N

        toJText.setFont(resourceMap.getFont("toJText.font")); // NOI18N
        toJText.setName("toJText"); // NOI18N

        toJLabel.setFont(resourceMap.getFont("toJLabel.font")); // NOI18N
        toJLabel.setText(resourceMap.getString("toJLabel.text")); // NOI18N
        toJLabel.setName("toJLabel"); // NOI18N

        subjectJLabel.setFont(resourceMap.getFont("subjectJLabel.font")); // NOI18N
        subjectJLabel.setText(resourceMap.getString("subjectJLabel.text")); // NOI18N
        subjectJLabel.setName("subjectJLabel"); // NOI18N

        subjectJText.setFont(resourceMap.getFont("subjectJText.font")); // NOI18N
        subjectJText.setName("subjectJText"); // NOI18N

        attachmentJLabel1.setFont(resourceMap.getFont("attachmentJLabel1.font")); // NOI18N
        attachmentJLabel1.setText(resourceMap.getString("attachmentJLabel1.text")); // NOI18N
        attachmentJLabel1.setName("attachmentJLabel1"); // NOI18N

        jScrollPane.setName("jScrollPane"); // NOI18N

        messagejTextArea.setColumns(20);
        messagejTextArea.setFont(resourceMap.getFont("messagejTextArea.font")); // NOI18N
        messagejTextArea.setLineWrap(true);
        messagejTextArea.setRows(5);
        messagejTextArea.setName("messagejTextArea"); // NOI18N
        jScrollPane.setViewportView(messagejTextArea);

        clearButton.setText(resourceMap.getString("clearButton.text")); // NOI18N
        clearButton.setName("clearButton"); // NOI18N
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        sendButton.setText(resourceMap.getString("sendButton.text")); // NOI18N
        sendButton.setName("sendButton"); // NOI18N
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        exitButton.setText(resourceMap.getString("exitButton.text")); // NOI18N
        exitButton.setMargin(new java.awt.Insets(10, 14, 10, 14));
        exitButton.setName("exitButton"); // NOI18N
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        sentMessage.setText(resourceMap.getString("sentMessage.text")); // NOI18N
        sentMessage.setName("sentMessage"); // NOI18N

        serverButton.setText(resourceMap.getString("serverButton.text")); // NOI18N
        serverButton.setMargin(new java.awt.Insets(10, 14, 10, 14));
        serverButton.setName("serverButton"); // NOI18N
        serverButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverButtonActionPerformed(evt);
            }
        });

        smtpServer.setText(resourceMap.getString("smtpServer.text")); // NOI18N
        smtpServer.setName("smtpServer"); // NOI18N

        portNumber.setText(resourceMap.getString("portNumber.text")); // NOI18N
        portNumber.setName("portNumber"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                        .addGap(124, 124, 124)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(smtpServer, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                            .addComponent(portNumber, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                        .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(toJLabel)
                            .addComponent(fromJLabel)
                            .addComponent(attachmentJLabel1)
                            .addComponent(subjectJLabel)
                            .addComponent(attachmentJLabel))
                        .addGap(10, 10, 10)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(sentMessage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                            .addComponent(subjectJText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                            .addComponent(toJText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                            .addComponent(fromJText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addComponent(attachmentJText, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(attachmentNameJText, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(browseButton))
                            .addComponent(jScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE))))
                .addGap(65, 65, 65))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fromJText, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fromJLabel))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(toJText, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toJLabel))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(subjectJText, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subjectJLabel))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(attachmentJText, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(attachmentNameJText, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(attachmentJLabel))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(attachmentJLabel1)
                    .addComponent(jScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sentMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(smtpServer, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(portNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serverButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(smtpclient.SMTPClientApp.class).getContext().getActionMap(SMTPClientView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 434, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        jFileChooser.setName("jFileChooser"); // NOI18N

        serverOptions.setMinimumSize(new java.awt.Dimension(335, 250));
        serverOptions.setName("serverOptions"); // NOI18N

        smtpServerText.setText(resourceMap.getString("smtpServerText.text")); // NOI18N
        smtpServerText.setName("smtpServerText"); // NOI18N
        smtpServerText.setPreferredSize(new java.awt.Dimension(59, 37));

        portText.setText(resourceMap.getString("portText.text")); // NOI18N
        portText.setName("portText"); // NOI18N
        portText.setPreferredSize(new java.awt.Dimension(59, 37));

        Submit.setText(resourceMap.getString("Submit.text")); // NOI18N
        Submit.setName("Submit"); // NOI18N
        Submit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SubmitActionPerformed(evt);
            }
        });

        jLabel1.setFont(resourceMap.getFont("jLabel1.font")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setFont(resourceMap.getFont("jLabel2.font")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        javax.swing.GroupLayout serverOptionsLayout = new javax.swing.GroupLayout(serverOptions.getContentPane());
        serverOptions.getContentPane().setLayout(serverOptionsLayout);
        serverOptionsLayout.setHorizontalGroup(
            serverOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(serverOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(Submit)
                    .addGroup(serverOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(smtpServerText, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(portText, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        serverOptionsLayout.setVerticalGroup(
            serverOptionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(smtpServerText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(portText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(Submit)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
    int returnVal = jFileChooser.showOpenDialog(browseButton);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = jFileChooser.getSelectedFile();
        try {
          // What to do with the file, e.g. display it in a TextArea
          attachmentJText.read( new FileReader( file.getAbsolutePath() ), null );
          attachmentJText.setText(file.getAbsolutePath());
          attachmentNameJText.setText(file.getName());

          
        } catch (IOException ex) {
          System.out.println("Problem accessing file"+file.getAbsolutePath());
        }
    } else {
        System.out.println("File access cancelled by user.");
    }
}//GEN-LAST:event_browseButtonActionPerformed

private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
    attachmentJText.setText("");
    attachmentNameJText.setText("");
    fromJText.setText("");
    toJText.setText("");
    subjectJText.setText("");
    messagejTextArea.setText("");
}//GEN-LAST:event_clearButtonActionPerformed

private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
    String mailTo = toJText.getText();
    String mailFrom = fromJText.getText();
    String subject = subjectJText.getText();
    String message = messagejTextArea.getText();    
    String attachmentName = attachmentNameJText.getText();
    String attachmentPath = attachmentJText.getText();
    
    sendMail(mailFrom, mailTo, subject, message, attachmentPath, attachmentName);
}//GEN-LAST:event_sendButtonActionPerformed

private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
    System.exit(0);
}//GEN-LAST:event_exitButtonActionPerformed

private void serverButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverButtonActionPerformed
    serverOptions.setVisible(true);
}//GEN-LAST:event_serverButtonActionPerformed

private void SubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SubmitActionPerformed
    smtp = smtpServerText.getText();
    port = Integer.parseInt(portText.getText());
    if(smtp.equals("") || port == 0) {
        smtp = "smtp.sbb.rs";
        port = 25;
        smtpServer.setText("SMTP server: " + smtp);
        portNumber.setText("Broj porta: " + port);
        serverOptions.setVisible(false);
    }
    else {
        serverOptions.setVisible(false);
        smtpServer.setText("SMTP server: " + smtp);
        portNumber.setText("Broj porta: " + port);
    }
}//GEN-LAST:event_SubmitActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Submit;
    private javax.swing.JLabel attachmentJLabel;
    private javax.swing.JLabel attachmentJLabel1;
    private javax.swing.JTextField attachmentJText;
    private javax.swing.JTextField attachmentNameJText;
    private javax.swing.JButton browseButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JButton exitButton;
    private javax.swing.JLabel fromJLabel;
    private javax.swing.JTextField fromJText;
    private javax.swing.JFileChooser jFileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextArea messagejTextArea;
    private javax.swing.JLabel portNumber;
    private javax.swing.JTextField portText;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel sentMessage;
    private javax.swing.JButton serverButton;
    private javax.swing.JDialog serverOptions;
    private javax.swing.JLabel smtpServer;
    private javax.swing.JTextField smtpServerText;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel subjectJLabel;
    private javax.swing.JTextField subjectJText;
    private javax.swing.JLabel toJLabel;
    private javax.swing.JTextField toJText;
    // End of variables declaration//GEN-END:variables

    
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    private JDialog serverBox; 
}
