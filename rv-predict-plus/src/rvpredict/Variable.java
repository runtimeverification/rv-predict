package rvpredict;

import rvpredict.logging.Protos;

import java.io.Serializable;

// I wish protobufs were Serializable
public final class Variable implements Serializable {
  private static final long serialVersionUID = 0;
  public enum Type { FieldAcc, ArrayAcc, StaticFieldAcc, ImpureCall }
  private final Type type;
  private final int objectID;
  private final String className;
  private final String fieldName;
  private final int index;
  @Override public int hashCode() { assert false: "Unimplemented"; return 0; }
  @Override public boolean equals(final Object o) { assert false: "Unimplemented"; return false; }
  Type getType() { return type; }
  int getObjectID() { return objectID; }
  String getClassName() { return className; }
  String getFieldName() { return fieldName; }
  int getIndex() { return index; }
  Variable(final Protos.Variable v) {
    switch (v.getType()) {
      case StaticFieldAcc:
        type = Type.StaticFieldAcc;
        className = v.getClassName();
        fieldName = v.getFieldName();
        objectID = 0;
        index = 0;
        break;
      case FieldAcc:
        type = Type.FieldAcc;
        className = v.getClassName();
        fieldName = v.getFieldName();
        objectID = v.getObjectID();
        index = 0;
        break;
      case ArrayAcc:
        type = Type.ArrayAcc;
        className = v.getClassName();
        fieldName = null;
        objectID = v.getObjectID();
        index = v.getIndex();
        break;
      case ImpureCall:
        type = Type.ImpureCall;
        className = v.getClassName();
        objectID = v.getObjectID();
        index = 0;
        fieldName = null;
        break;
      default:
        assert false: "Unimplemented, Variable was: \n" + v;
        type = null;
        objectID = 0;
        className = null;
        fieldName = null;
        index = 0;
        break;
    }
  }
  @Override public String toString() {
    switch (getType()) {
      case FieldAcc:
        return getClassName()+"."+getFieldName()+" (instance #"+getObjectID()+")";
      case StaticFieldAcc:
        return getClassName()+"."+getFieldName();
      case ArrayAcc:
        return getClassName()+"["+getIndex()+"] (instance #"+getObjectID()+")";
      case ImpureCall:
        return getClassName()+" (instance #"+getObjectID()+")";
      default:
        assert false: "Tried to pretty print unsupported variable type, "+getType();
        System.exit(1);
        return null;
    }
  }
}
// vim: tw=100:sw=2
