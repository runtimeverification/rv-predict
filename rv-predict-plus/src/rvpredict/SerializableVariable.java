//This contains the same data as a Protos.Variable, but is actually Serializable in a safe
//way because it uses value semantics for hashCode and equals

package rvpredict;

import rvpredict.logging.Protos;

public class SerializableVariable implements java.io.Serializable {
  static final long serialVersionUID = 0L;

  private final Protos.Variable.Type type;
  public Protos.Variable.Type getType() { return type; }
  private final String className;
  public String getClassName() { return className; }
  private final String fieldName;
  public String getFieldName() { return fieldName; }
  private final int objectID;
  public int getObjectID() { return objectID; }
  private final int index;
  public int getIndex() { return index; }
  private int hash;

  public SerializableVariable(Protos.Variable v){
    type = v.getType();
    className = v.getClassName();
    fieldName = v.getFieldName();
    objectID = v.getObjectID();
    index = v.getIndex();
    hash = type.getNumber() ^ className.hashCode() ^ fieldName.hashCode() ^ objectID ^ index;
  }

  @Override public int hashCode(){
    return hash;
  }

  @Override public boolean equals(Object o){
    if(!(o instanceof SerializableVariable)) return false;
    SerializableVariable v = (SerializableVariable)o;
    return (type == v.getType() 
         && objectID == v.getObjectID() 
         && index == v.getIndex()
         && fieldName.equals(v.getFieldName())
         && className.equals(v.getClassName())); 
  }
}
