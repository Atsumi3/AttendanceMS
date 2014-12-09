package info.nukoneko.attendancems.common;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Atsumi on 2014/11/10.
 */
public class ReadNFC {
    public static String readNFC(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NfcF techF = NfcF.get(tag);
        try {
            techF.connect();
            // android の NFC機能にアクセス
            if (techF.isConnected()) {
                // polling
                ResultPollingObject result = nfcPolling(techF, techF.getSystemCode());
                if(result != null && result.isSuccess()) {
                    ResultReadObject readResult = nfcReadWithoutEncryption(techF, result.IDm, (byte)0x8004);
                    if (readResult!= null && readResult.isSuccess()) {
                        return getStudentID(readResult.blockData.get(0));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static class ResultPollingObject {
        private boolean success;
        byte[] IDm = new byte[8];
        byte[] PMm = new byte[8];
        public boolean isSuccess(){
            return this.success;
        }
        public ResultPollingObject(byte[] result) {
            this.success = result[1] == 0x01;
            if (this.success) {
                System.arraycopy(result, 2, this.IDm, 0 , 8);
                System.arraycopy(result, 10, this.PMm, 0 , 8);
            }
        }
    }
    private static class ResultReadObject {
        private boolean success;
        byte[] IDm = new byte[8];
        byte statusFlag1;
        byte statusFlag2;
        int blockNum;
        ArrayList<byte[]> blockData = new ArrayList<byte[]>();
        public boolean isSuccess(){
            return this.success;
        }
        public ResultReadObject(byte[] result){
            this.success = result[1] == 0x07;
            if (this.success) {
                System.arraycopy(result, 2, this.IDm, 0, 8);
                this.statusFlag1 = result[10];
                this.statusFlag2 = result[11];
                if(this.statusFlag1 == 0x00){
                    this.blockNum = (int)result[12];
                    for(int i = 0 ; i < this.blockNum; i++){
                        byte[] dat = new byte[16];
                        System.arraycopy(result, 12 * (1 + i) + 1, dat, 0, 16);
                        blockData.add(dat);
                    }
                }
            }
        }
    }
    private static ResultPollingObject nfcPolling(NfcF nfcF, byte[] SYSTEM_CODE) {
        try {
            return new ResultPollingObject(
                    nfcF.transceive(new byte[]{
                            (byte) 0x06, // data size
                            (byte) 0x00, // command polling -> 00
                            SYSTEM_CODE[0], // system code
                            SYSTEM_CODE[1], // system code
                            (byte) 0x00, // request code
                            (byte) 0x0F // time slot
                    })
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static ResultReadObject nfcReadWithoutEncryption(NfcF nfcF, byte[] IDm, byte BLOCK) {
        try {
            return new ResultReadObject(
                    nfcF.transceive(new byte[]{
                            (byte) 0x10, // data size
                            (byte) 0x06, // command polling -> 00
                            IDm[0],
                            IDm[1],
                            IDm[2],
                            IDm[3],
                            IDm[4],
                            IDm[5],
                            IDm[6],
                            IDm[7],
                            (byte) 0x01,
                            (byte) 0x0b,
                            (byte) 0x00,
                            (byte) 0x01, // read block num
                            (byte) 0x80,
                            BLOCK // block code
                    })
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String getStudentID(byte[] bytes){
        byte[] ids = new byte[7];
        System.arraycopy(bytes, 2, ids, 0, 7);
        return byte2String(ids, (byte)0x0F);
    }
    private static String byte2String(byte[] bytes, byte mask) {
        StringBuilder ret = new StringBuilder();
        if ( bytes != null) {
            for (byte aByte : bytes) {
                ret.append(Integer.toHexString(aByte & mask)) ;
            }
        }
        return ret.toString();
    }
}
