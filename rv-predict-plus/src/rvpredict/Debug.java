package rvpredict;
public enum Debug {
  Slicing, VectorClocks;
  static public void Slicing(String output) { if (Main.debugSlicing) System.out.println(output); }
  static public void VectorClocks(String output) { if (Main.debugVectorClocks) System.out.println(output); }
}
