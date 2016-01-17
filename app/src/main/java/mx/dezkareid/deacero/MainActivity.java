package mx.dezkareid.deacero;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter adapter;

    private Tag tag;
    Parcelable[] parcelables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(!adapterWorks())
            return;
        if(intent.hasExtra(adapter.EXTRA_TAG)){
            handleIntent(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!adapterWorks())
            return;
        enableSystemForegroundNFC();
    }

    public boolean adapterWorks(){
        if(adapter == null){
            return false;
        }
        return adapter.isEnabled();
    }

    private void enableSystemForegroundNFC(){
        Intent intent = new Intent(this,this.getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        IntentFilter[] intentFilter = new IntentFilter[]{new IntentFilter(adapter.ACTION_NDEF_DISCOVERED), new IntentFilter(adapter.ACTION_TECH_DISCOVERED), new IntentFilter(adapter.ACTION_TAG_DISCOVERED) };
        adapter.enableForegroundDispatch(this,pendingIntent,intentFilter,null);
    }

    public void enviaMensaje(String codigoCliente){
        String numero = getString(R.string.num_tel);
        String asunto = getString(R.string.asunto);
        SmsManager smsManager = SmsManager.getDefault();
        try{
            smsManager.sendTextMessage(numero,null,asunto+" "+codigoCliente,null,null);
            enviaRequest("http://192.168.1.51:1337/map/avisa");

        }catch (Exception e){
            Toast.makeText(this,"Error al registrar el check-in "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }


    public void enviaRequest(String url){
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    public String getTextFromRecord(NdefRecord ndefRecord){
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload,languageSize+1,
                    payload.length -languageSize -1,textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("TextFromRecord",e.getMessage(),e);
        }
        return tagContent;
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            parcelables = intent.getParcelableArrayExtra(adapter.EXTRA_NDEF_MESSAGES);
            readNFC();

        }
    }

    public void readNFC(){
        if(parcelables != null){
            if(parcelables.length>0){
                readTextFromMessage((NdefMessage) parcelables[0]);
            }
        }
    }

    private void readTextFromMessage(NdefMessage ndefMessage){
        NdefRecord[] ndefRecords = ndefMessage.getRecords();
        if(ndefRecords != null && ndefRecords.length>0){
            NdefRecord ndefRecord = ndefRecords[0];

            String codigoCliente = getTextFromRecord(ndefRecord);
            enviaMensaje(codigoCliente);
        }
    }

}
