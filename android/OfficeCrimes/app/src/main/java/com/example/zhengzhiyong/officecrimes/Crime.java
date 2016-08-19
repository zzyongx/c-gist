package com.example.zhengzhiyong.officecrimes;

import java.util.Date;
import java.util.UUID;

public class Crime {
  UUID mId;
  String mTitle;
  Date mDate;
  boolean mSolved;
  
  public Crime() {
    mId = UUID.randomUUID();
    mDate = new Date();
  }

  public void setTitle(String title) {
    mTitle = title;
  }
  public String getTitle() {
    return mTitle;
  }

  public UUID getId() {
    return mId;
  }

  public Date getDate() {
    return mDate;
  }
  public void setDate(Date date) {
    mDate = date;
  }

  public boolean getSolved() {
    return mSolved;
  }
  public void setSolved(boolean solved) {
    mSolved = solved;
  }
}
