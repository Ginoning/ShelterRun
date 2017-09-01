package ginoning.shelterrun.model;

import android.content.Context;
import android.telephony.SmsManager;

/**
 * Created by bgh29 on 2017-09-02.
 */

public class SMSSender {
    private Context mCon;
    public SMSSender(Context con){
        mCon = con;
    }

    public void sendMessage(String message){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("01028550663", null, message, null, null);
    }
}
