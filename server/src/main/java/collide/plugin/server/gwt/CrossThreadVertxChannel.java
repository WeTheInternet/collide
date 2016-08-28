package collide.plugin.server.gwt;

import com.google.collide.dtogen.server.JsonSerializable;
import com.google.collide.server.shared.util.Dto;
import com.google.collide.server.shared.util.ReflectionChannel;
import io.vertx.core.eventbus.EventBus;

import java.util.Stack;

public class CrossThreadVertxChannel extends ReflectionChannel{

  private final Stack<String> jsonString = new Stack<String>();
  private EventBus eb;
  private String address;
  public CrossThreadVertxChannel(ClassLoader cl, EventBus eb,String address) {
    super(cl, null);
    this.eb = eb;
    this.address = address;
  }
  /**
   * @return a json string to ease communication between threads with different classloaders.
   * The only time in() is called is to get the gwt compile input json,
   * which is an instance of @GwtRecompile
   *
   */
    @Override
    public String receive() {
      if (jsonString.isEmpty())
        return null;
      return jsonString.pop();
    }

    public void setOutput(String next){
      jsonString.push(next);
    }

    /**
     * @param msg - A json encoded message to send to client.
     * If the request message had a replyAddress,
     * we pipe first output as a json reply to that message.
     * All subsequent messages to gwt.log (this.address)
     */
    @Override
    public void send(String msg) {
        if (msg.startsWith("_")){//this message has packed an alternate address to use.
          int ind = msg.indexOf('_',1);
          if (ind > 0){
            String to = msg.substring(1,ind);
            eb.send(to,Dto.wrap(msg.substring(ind+1)));
            return;
          }
        }
        //sends as gwt.log
        eb.send(address,Dto.wrap(msg));
    }
    public static String encode(String address, JsonSerializable message) {
      assert !address.contains("_") : "You may not use _ in addresses sent through CrossThreadVertxChannel";
      String encoded = message.toJson();
      return "_"+address+"_"+encoded;
    }

}
