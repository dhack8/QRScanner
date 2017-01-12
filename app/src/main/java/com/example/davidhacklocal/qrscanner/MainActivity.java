package com.example.davidhacklocal.qrscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.Result;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView ScannerView;
    private float width;
    private float height;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
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
        String resultString = result.getText();

        if(resultString.startsWith("MATMSG") && resultString.endsWith(";;")) { //Email QR Code
            email(resultString);
        }else if(resultString.startsWith("BEGIN:VCARD") /*&& resultString.endsWith("END:VCARD")*/){ //Contact QR Code
            contact(resultString);
        }else{ //Link/Text QR Code
            text(resultString);
        }
    }

    public void email(String result){
        final String to = result.substring(10, result.indexOf(";SUB:"));
        final String subject = result.substring(result.indexOf(";SUB:")+5, result.indexOf(";BODY:"));
        final String body = result.substring(result.indexOf(";BODY:")+6, result.length()-2);
        final String description = "To: " + to +"\n"+ "Subject: " + subject+"\n"+"Body:\n"+body;

        TextView textView = customTextView(description);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Result - Email")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() { //Send email
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/html");
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {to});
                        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                        intent.putExtra(Intent.EXTRA_TEXT, body);
                        startActivity(Intent.createChooser(intent, "Send Email"));
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setNegativeButton("Copy", new DialogInterface.OnClickListener() { //Copy contents
                    public void onClick(DialogInterface dialog, int id) {
                        copyText(description);
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setNeutralButton("Close", new DialogInterface.OnClickListener() { //Exit dialog
                    public void onClick(DialogInterface dialog, int id) {
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setView(textView);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void contact(final String result){
        TextView textView = customTextView(result);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Result - vCard")
                .setPositiveButton("Add to Contacts", new DialogInterface.OnClickListener() { //Send email
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        File vcfFile = new File(getExternalFilesDir(null), "QRScanner.vcf");
                        try {
                            FileWriter fw = new FileWriter(vcfFile);
                            fw.write(result);
                            fw.close();
                            intent.setDataAndType(Uri.fromFile(vcfFile), "text/x-vcard");
                            startActivity(intent);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setNegativeButton("Copy", new DialogInterface.OnClickListener() { //Copy contents
                    public void onClick(DialogInterface dialog, int id) {
                        copyText(result);
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setNeutralButton("Close", new DialogInterface.OnClickListener() { //Exit dialog
                    public void onClick(DialogInterface dialog, int id) {
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setView(textView);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void text(final String result){
        TextView textView = customTextView(result);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Result - Text")
                .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        copyText(result);
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ScannerView.resumeCameraPreview(MainActivity.this); //Resume scanning
                    }
                })
                .setView(textView);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public TextView customTextView(String text){
        final TextView tx1 = new TextView(this);
        tx1.setPadding((int)(0.16*width),(int)(0.05*height),(int)(0.16*width),(int)(0.05*height));
        tx1.setText(text);
        tx1.setAutoLinkMask(RESULT_OK);
        tx1.setMovementMethod(LinkMovementMethod.getInstance());
        return tx1;
    }

    public void copyText(String text){
        Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        CharSequence cs = text;
        ClipData clip = ClipData.newPlainText("Scan", cs);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(MainActivity.this, "Permission denied to use camera", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
            }
        }
    }
}
