package com.example.davidhacklocal.qrscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class MainActivityOld extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView ScannerView;
    private float width;
    private float height;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(MainActivityOld.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivityOld.this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        super.onCreate(savedInstanceState);
        ScannerView = new ZXingScannerView(this);
        setContentView(ScannerView);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        width = metrics.xdpi;
        height = metrics.ydpi;
    }

    @Override
    protected void onPause(){
        super.onPause();
        ScannerView.stopCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        ScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        ScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void handleResult(Result result){
        //Handle result here
        final SpannableString resultString = new SpannableString(result.getText());
        Log.v("handleResult", resultString.toString());
        Log.v("handleResult", result.getBarcodeFormat().toString());

        if(resultString.toString().startsWith("MATMSG")){
            String[] items = resultString.toString().split(";");

            String to = items[0].split(":")[2];
            String subject = items[1].split(":")[1];
            String body = items[2].split(":")[1];

            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setTitle("Mail To");
            builder1.setMessage("To: " + to +"\n"+ "Subject: " + subject+"\n"+"Body: "+body);
            AlertDialog alertDialog1 = builder1.create();
            alertDialog1.show();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {to});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);

            startActivity(Intent.createChooser(intent, "Send Email"));
            ScannerView.resumeCameraPreview(MainActivityOld.this);
            return;
        }

        final TextView tx1 = new TextView(this);
        tx1.setPadding((int)(0.16*width),(int)(0.05*height),(int)(0.16*width),(int)(0.05*height));
        tx1.setText(resultString);
        tx1.setAutoLinkMask(RESULT_OK);
        tx1.setMovementMethod(LinkMovementMethod.getInstance());

        Linkify.addLinks(resultString, Linkify.ALL);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Result")
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(MainActivityOld.this, "Copied", Toast.LENGTH_SHORT).show();
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        CharSequence cs = resultString;
                        ClipData clip = ClipData.newPlainText("Scan", cs);
                        clipboard.setPrimaryClip(clip);
                        //Resume scanning
                        ScannerView.resumeCameraPreview(MainActivityOld.this);
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Resume scanning
                        ScannerView.resumeCameraPreview(MainActivityOld.this);
                    }
                })
                .setView(tx1);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        //Resume scanning
        //ScannerView.resumeCameraPreview(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(MainActivityOld.this, "Permission denied to use camera", Toast.LENGTH_SHORT).show();
                    this.finishAffinity();
                }
            }
        }
    }
}
