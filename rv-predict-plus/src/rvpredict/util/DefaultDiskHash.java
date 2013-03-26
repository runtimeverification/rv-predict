package rvpredict.util;

public class DefaultDiskHash<KeyT, ValT> extends DiskHash<KeyT, ValT> {
  private ValT defaultVal;

  public DefaultDiskHash(final ValT defaultVal){
    this.defaultVal = defaultVal;
  }

  @Override
  public ValT get(KeyT k){
     ValT ret = super.get(k);
     if (ret == null){
       put(k, defaultVal);
       return defaultVal;
     }
     return ret;
  }
}
