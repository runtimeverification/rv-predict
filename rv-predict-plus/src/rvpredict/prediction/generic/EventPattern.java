package rvpredict.prediction.generic;

public class EventPattern {
  String loc;
  boolean isRead;
	
  public EventPattern(String loc, boolean isRead){
    this.loc = loc;
    this.isRead = isRead;
  }

  public boolean match(Event event, String field){
    if ((event.eventType != Event.FIELD) && (event.eventType != Event.ELEMENT))
      return false;
    String l = event.getLoc();
    if (field.length() > 0)
      l += "." + field;
    return ((isRead == event.isRead) && (loc.compareTo(l) == 0));
  }
}
// vim: tw=100:sw=2
