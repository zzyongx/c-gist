package com.example.zhengzhiyong.officecrimes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.content.Context;

public class CrimeLab {
  private static CrimeLab ourInstance = null;
  private Context mContext;
  private List<Crime> mCrimes;

  public static CrimeLab getInstance(Context c) {
    if (ourInstance == null) {
      ourInstance = new CrimeLab(c.getApplicationContext());
    }
    return ourInstance;
  }

  public List<Crime> getCrimes() {
    return mCrimes;
  }

  public Crime getCrime(UUID crimeId) {
    for (Crime crime : mCrimes) {
      if (crime.getId().equals(crimeId)) return crime;
    }
    return null;
  }
  
  private CrimeLab(Context c) {
    mContext = c;
    mCrimes = new ArrayList<Crime>();
    for (int i = 0; i < 20; ++i) {
      Crime crime = new Crime();
      crime.setTitle("Crime #" + i);
      crime.setSolved(i % 2 == 0);
      mCrimes.add(crime);
    }
  }
}
