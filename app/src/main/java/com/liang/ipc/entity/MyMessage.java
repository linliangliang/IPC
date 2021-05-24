package com.liang.ipc.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MyMessage implements Parcelable {
    private String mContent;
    private boolean isSendSuccess;

    public MyMessage(){

    }

    protected MyMessage(Parcel in) {
        mContent = in.readString();
        isSendSuccess = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mContent);
        dest.writeByte((byte) (isSendSuccess ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MyMessage> CREATOR = new Creator<MyMessage>() {
        @Override
        public MyMessage createFromParcel(Parcel in) {
            return new MyMessage(in);
        }

        @Override
        public MyMessage[] newArray(int size) {
            return new MyMessage[size];
        }
    };

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public boolean isSendSuccess() {
        return isSendSuccess;
    }

    public void setSendSuccess(boolean sendSuccess) {
        isSendSuccess = sendSuccess;
    }

    public void readFromParcel(Parcel parcel) {
         mContent = parcel.readString();
         isSendSuccess = parcel.readByte() == 1;
    }
}
