package com.example.refrigeratorgo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class AddfoodActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public String editValue = null;
    private String url = "https://www.cvslove.com/product/product_view.asp?pcode=";
    private ImageView home_btn, recipe_btn, camera_btn, alarm_btn, user_btn;

    TextView editCategory;
    EditText editName, editDate, editMemo;
    Button btnAdd;
    Spinner spinner;
    String[] item;
    String barcode;
    ImageView imageView;
    private ImageButton btnChoose,  scan_btn;

    private TextView contentTxt;
    private static final  int IMAGE_PICK_CODE = 1000;
    private static final  int PERMISSION_CODE = 1001;
    private static int PICK_IMAGE_REQUEST = 1;
    private static int PICK_FROM_BARCODE = 1;
    final int PICK_FROM_ALBUM = 999;
    private IntentIntegrator integrator;

    public static SQLiteHelper sqLiteHelper;

    @Override
    protected void  onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addfood);
        init();

        sqLiteHelper = new SQLiteHelper(this, "FoodDB.sqlite", null, 1); // DB ??????

        sqLiteHelper.queryData("CREATE TABLE IF NOT EXISTS FOOD (Id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, date VARCHAR, image BLOB, category VARCHAR, memo VARCHAR)");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        //final Activity activity = this;
        integrator = new IntentIntegrator(this);

        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);  ??????????????? ?????????(???????????? qr????????? ??????)
                integrator.setCaptureActivity(CaptureActivityAnyOrientation.class); // ????????? ???????????? ??????
                integrator.setOrientationLocked(false);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false); //true??? ???????????? ????????? '???'?????????
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        //Views

        //handle button click
        btnChoose.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //check runtime permission
                ActivityCompat.requestPermissions(
                        AddfoodActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PICK_FROM_ALBUM
                );
            }
        });

        //?????? ??????
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    sqLiteHelper.insertDataFood(
                            editName.getText().toString().trim(),
                            editDate.getText().toString().trim(),
                            imageViewToByte(imageView),
                            editCategory.getText().toString().trim(),
                            editMemo.getText().toString().trim()
                    );
                    Toast.makeText(getApplicationContext(), "???????????? ??????????????? ?????????????????????!", Toast.LENGTH_SHORT).show();
                    editName.setText("");
                    editDate.setText("");
                    imageView.setImageResource(R.drawable.background5);
                    editCategory.setText("");
                    editMemo.setText("");
                    sqLiteHelper.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        home_btn = (ImageView) findViewById(R.id.page_home);
        home_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddfoodActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //?????? ??????(.setFlags)
                startActivity(intent);
            }
        });

        recipe_btn = (ImageView) findViewById(R.id.recipes_book);
        recipe_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddfoodActivity.this, RecipesMain.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //?????? ??????(.setFlags)
                startActivity(intent);
            }
        });

        camera_btn = (ImageView) findViewById(R.id.plus_camera);
        camera_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddfoodActivity.this, AddfoodActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //?????? ??????(.setFlags)
                startActivity(intent);
            }
        });

        //??????
        alarm_btn = (ImageView) findViewById(R.id.alarm);
        alarm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddfoodActivity.this, NotificationChannelCreate.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //?????? ??????(.setFlags)
                startActivity(intent);
            }
        });

        user_btn = (ImageView) findViewById(R.id.users);
        user_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AddfoodActivity.this, UserSetMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //?????? ??????(.setFlags)
                startActivity(intent);
            }
        });
    }

    public static byte[] imageViewToByte(ImageView image) {
        Bitmap bitmap = ( (BitmapDrawable)image.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

   /* @Override
    protected void onResume() { // ????????? ????????? ??? ???????????? ????????? ??????!
        super.onResume();
        Description task = new Description();
        task.execute();
        Description2 task2 = new Description2();
        task2.execute();
    }*/

    //handle result of runtime permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PICK_FROM_ALBUM){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_FROM_ALBUM);
            }
            else{
                Toast.makeText(getApplicationContext(), "You dont have permission to access file locationl", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //handle result of picked image
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
        editCategory.setText(item[i]);
        if(editCategory.getText().toString().equals("???????????????")){
            editCategory.setText("");
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView){
        editCategory.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //??????????????? ????????? ????????????
        if (requestCode == PICK_FROM_ALBUM) {
            if (resultCode == RESULT_OK && null != data){
                Uri uri = data.getData(); // data?????? ???????????? ????????? ?????????

                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 150, 150, true); //150??? ????????? ?????? ??????
                    imageView.setImageBitmap(scaled);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                super.onActivityResult(requestCode, resultCode, data);
            }
        }
        //??????????????? ??? ?????? ?????? ??????
        else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            barcode = result.getContents();
            if (barcode == null) {
                Toast.makeText(this, "You cancelled this scanning", Toast.LENGTH_LONG).show();
            } else {
                contentTxt.setText(barcode);
                new Description().execute(); // ????????? ??????
                new Description2().execute(); // ?????? ????????? ??????
            }
        }
    }

    private class Description extends AsyncTask<String, String, String> { // ???????????? ????????? ????????? ?????? ??????
        String productName;
        //  private ImageView Image = null;
        @Override
        protected String doInBackground(String... params) {
            try {
                String URL = url+barcode;
                Document document = Jsoup.connect(URL).get();
                Elements mElementDataSize = ((Document) document).select("table[ID=Table4]"); //"table[ID=Table3]"
                for (Element elem : mElementDataSize) {
                    productName = elem.select("td[width=60%]").first().text(); //.toString();
                    //String img_url = elem.select("td[style=padding-left:20px;]").attr("src");
                    //Glide.with(AddfoodActivity.this).load(img_url).override(150,150).skipMemoryCache(true).into(mImageView);
                    //pdtName.add(productName);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("????????????", "catch??? ??????");
            }
            return productName;
        }

        @Override
        protected void onPostExecute(String productName) {
            //doInBackground ????????? ????????? ????????? ??????
            editName.setText(productName);
        }
    }

    private class Description2 extends AsyncTask<String, String, String> { // ???????????? ????????? ????????? ????????? ??????
        String img_url;
        //  private ImageView Image = null;
        @Override
        protected String doInBackground(String... params) {
            try {
                String URL = url+barcode;
                Document document = Jsoup.connect(URL).get();
                Elements mElementDataSize = ((Document) document).select("table[ID=Table3]"); //"table[ID=Table3]"
                for (Element elem : mElementDataSize) {
                    img_url = elem.select("img").attr("src");
                    //String img_url = elem.select("td[style=padding-left:20px;]").attr("src");
                    //Glide.with(AddfoodActivity.this).load(img_url).override(150,150).skipMemoryCache(true).into(mImageView);
                    //pdtName.add(productName);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("????????????", "catch??? ??????");
            }
            return img_url;
        }

        @Override
        protected void onPostExecute(String img_url) {
            Glide.with(AddfoodActivity.this).load(img_url).override(150,150).skipMemoryCache(true).into(imageView); //glide??? ????????? ????????? ??????
        }
    }

    private void init(){
        spinner=(Spinner)findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        item= new String[]{"???????????????", "?????? / ?????????", "?????? / ??????", "????????? / ?????????","?????? / ?????? / ????????????","?????? / ??????", "?????????","????????? / ?????????","?????? / ?????? / ???","??????"};
        contentTxt = (TextView)findViewById(R.id.scan_content);
        scan_btn =(ImageButton)findViewById(R.id.scan_btn);
        imageView = (ImageView) findViewById(R.id.image_view);

        editName = (EditText)findViewById(R.id.product_name);
        editDate = (EditText)findViewById(R.id.mYear);
        editCategory =(TextView)findViewById(R.id.selectedText);
        editMemo = (EditText)findViewById(R.id.edtMemo);

        btnChoose = (ImageButton)findViewById(R.id.choose_image_btn);
        btnAdd = (Button)findViewById(R.id.textbutton);
    }
}