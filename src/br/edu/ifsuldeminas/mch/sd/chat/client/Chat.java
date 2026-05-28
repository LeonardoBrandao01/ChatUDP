package br.edu.ifsuldeminas.mch.sd.chat.client;
import java.util.Scanner;
import br.edu.ifsuldeminas.mch.sd.chat.ChatException;
import br.edu.ifsuldeminas.mch.sd.chat.ChatFactory;
import br.edu.ifsuldeminas.mch.sd.chat.Sender;
public class Chat {

public static String KEY_TO_EXIT = "q";
public static int RECEIVER_BUFFER_SIZE = 1000;
public static void main(String[] args) {
Scanner reader = null;
int localPort = 0;
int serverPort = 0;
try {
localPort = Integer.parseInt(args[0]);
serverPort = Integer.parseInt(args[1]);
} catch (NumberFormatException e1) {
System.err.printf("Errors on imput parameters");
System.exit(1);
}
try {
Sender sender = ChatFactory.build(true, "localhost", serverPort,
localPort, new SysOutContainer());
reader = new Scanner(System.in);
String message = "";
while (!message.equals(KEY_TO_EXIT)) {
message = reader.nextLine();
if (!message.equals("q")) {
sender.send(message);
} else
System.exit(0);
}
} catch (NumberFormatException e) {
e.printStackTrace();
} catch (ChatException chatException) {
System.err.printf(String.format(
"There was a problem with the chat. Error message: %s",
chatException.getCause().getMessage()));
reader.close();
System.exit(0);
}
}
}