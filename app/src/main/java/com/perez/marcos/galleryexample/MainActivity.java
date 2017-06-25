package com.perez.marcos.galleryexample;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

import static android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_MANAGE_DOCUMENTS = 2;
    SharedPreferences sp;
    Button buttonContent, buttonPick, buttonChoose;
    ImageView image;
    private boolean canWeRead = false;
    private Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("galleryexample", Context.MODE_PRIVATE);

        buttonContent = (Button) findViewById(R.id.buttonActionGetContent);
        buttonContent.setOnClickListener(this);

        buttonPick = (Button) findViewById(R.id.buttonActionPick);
        buttonPick.setOnClickListener(this);

        buttonChoose = (Button) findViewById(R.id.buttonChooser);
        buttonChoose.setOnClickListener(this);

        image = (ImageView) findViewById(R.id.imageView);

        canWeRead = canWeRead();
        if(canWeRead) {
            loadImageFromString(sp.getString("imagePath",null));
        }
    }

    private boolean canWeRead(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED;
    }

    private void loadImageFromString (String imagePath){
        if(imagePath != null){
            Uri imageUri = Uri.parse(imagePath);
            loadImageFromUri(imageUri);
        }
    }

    private void loadImageFromUri(Uri imageUri) {
        try {
            image.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Intent getContentIntent() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT <19){
            intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        return intent;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonActionGetContent:
                /*
                    Apunte:
                    Si utilizamos ACTION.MANAGE_DOCUMENTS para escoger la foto tendremos que añadir unas líneas extras:
                    puedes leer sobre ello aquí: https://developer.android.com/guide/topics/providers/document-provider.html#permissions
                    Mejor utilizar el modo ACTION_PICK
                 */

                Intent getImageAsContent = getContentIntent();
                getImageAsContent.setType("image/*");
                startActivityForResult(getImageAsContent, 1);
                break;
            case R.id.buttonActionPick:

                /*
                * Esto funciona, pero no es correcto según la guía de Google
                * ya que ACTION_PICK espera una URI del contenido.
                *
                * Intent pickAnImage = new Intent(Intent.ACTION_PICK, null);
                * pickAnImage.setType("image/*");
                */

                /*Código correcto de action pick */
                PermissionUtils.checkReadExternalStoragePermissions(activity,MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                Intent pickAnImage = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickAnImage.setType("image/*");

                startActivityForResult(pickAnImage, 2);
                break;

            case R.id.buttonChooser:

                Intent getIntent = getContentIntent();
                getIntent.setType("image/*");

                //Este Intent define para el ACTION_PICK, la URI de donde cogerá los datos
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                //Usamos el Intent anterior para filtrar únicamente los que queremos que usen
                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, 3);
                break;
            default:
                Log.v("OnClick", "Not implemented");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Como en este caso los 3 intents hacen lo mismo, si el estado es correcto recogemos el resultado
        //Aún así comprobamos los request code. Hay que tener total control de lo que hace nuestra app.
        if(resultCode == RESULT_OK){
            if(requestCode >= 1 && requestCode <= 3){
                //Líneas extras debido al usar action get content:
                data.getData();
                Uri selectedImage = data.getData();
                String selectedImagePath = selectedImage.toString();

                if(canWeRead && requestCode == 2){
                    Log.v("PICK","Selected image uri" + selectedImage);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("imagePath",selectedImagePath );
                    editor.apply();
                }
                loadImageFromUri(selectedImage);
            }
        }else{
            Log.v("Result","Something happened");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    canWeRead = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    canWeRead = false;
                }
                return;
            }
            case  MY_PERMISSIONS_REQUEST_MANAGE_DOCUMENTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    canWeRead = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    canWeRead = false;
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
