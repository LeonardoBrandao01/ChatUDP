package br.edu.ifsuldeminas.mch.sd.chat.client;
import br.edu.ifsuldeminas.mch.sd.chat.MessageContainer;
public class SysOutContainer implements MessageContainer {
public void newMessage(String message) {
System.out.println(String.format(":> %s", message));
}
}