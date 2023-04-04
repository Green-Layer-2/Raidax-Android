package com.cloudcoin2.wallet.withdraw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.icu.lang.UCharacter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.Adapter.IndicatorAdapter;
import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.Utils.CommonUtils;
import com.cloudcoin2.wallet.Utils.Constants;
import com.cloudcoin2.wallet.Utils.CouldcoinApplication;
import com.cloudcoin2.wallet.Utils.KotlinUtils;
import com.cloudcoin2.wallet.Utils.NumberWordConverter;
import com.cloudcoin2.wallet.Utils.RAIDA;
import com.cloudcoin2.wallet.Utils.ScreeUtils;
import com.cloudcoin2.wallet.Utils.UDPCallBackInterface;
import com.cloudcoin2.wallet.base.BaseFragment2;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.SavedCoin;
import com.cloudcoin2.wallet.db.Transactions;
import com.cloudcoin2.wallet.deposit.DepositFragment;
import com.google.firebase.installations.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawFragment extends BaseFragment2 implements View.OnClickListener, UDPCallBackInterface {

    ImageView ivBack;
    private RecyclerView rvIndicator;
    private IndicatorAdapter mAdapter;
    private List<SavedCoin> lCoinCount = new ArrayList<>();
    private ArrayList<EchoStatus> mList = new ArrayList<>();
    private EditText etWithdraw;
    private LinearLayout llWithdraw;
    private TextView errormessage, totalPrice, tvExport;
    private EditText etMemo;
    private ImageView ivRefresh;
    private LinearLayout llProgress;
    ArrayList<SavedCoin> savedCoins = new ArrayList<SavedCoin>();
    static String BANK_DIR_NAME = "Bank";
    static String BANK_LIMBO_NAME = "Limbo";
    static String COUNTERFEIT_DIR_NAME = "Counterfeit";
    static String FRACKED_DIR_NAME = "Fracked";
    static String IMPORT_DIR_NAME = "Import";
    static String TRASH_DIR_NAME = "Trash";
    static String ID_DIR_NAME = "ID";
    static String DIR_BASE = "CloudCoins";
    static String EXPORT_DIR_NAME = "Export";
    private int echoPassed = 0;
    private String bankDirPath, limboDirPath, counterfeitPath, importPath, frackedDirPath, trashPath, exportPath;
    String error_message = "Requested Amount Should not be greater then Available Amount,please Retry Using Lower Amount";
    String error_no_amount = "Please Enter Amount";
    Handler mhandler;
    private int totalCoins = 0;
    private ArrayList<Uri> coinsToDelete = new ArrayList<>();

    @Override
    protected int defineLayoutResource() {
        return R.layout.fragment_withdraw;
    }

    @Override
    protected void initializeComponent(@NonNull View view) {
        ivBack = view.findViewById(R.id.fragment_withdraw_ivBack);
        ivBack.setOnClickListener(this);
        rvIndicator = view.findViewById(R.id.fragment_withdraw_rvIndicator);
        etWithdraw = view.findViewById(R.id.withdraw_Amount);
        etMemo = view.findViewById(R.id.fragment_withdraw_etStatus);
        llWithdraw = view.findViewById(R.id.fragment_withdraw_llExport);
        errormessage = view.findViewById(R.id.error_message);
        errormessage.setVisibility(View.GONE);
        llProgress = view.findViewById(R.id.fragment_withdraw_llProgress);
        llProgress.setVisibility(View.GONE);

        totalPrice = view.findViewById(R.id.withdraw_fragment_tvPrice);
        llWithdraw.setOnClickListener(this);
        tvExport = view.findViewById(R.id.export_txt);
        tvExport.setText("Withdraw");

        //ivRefresh.setVisibility(View.INVISIBLE);
        int size = ScreeUtils.INSTANCE.getScreenWidth(getBaseActivity()) -
                getResources().getDimensionPixelSize(R.dimen._220sdp);
        int itemWidth = (size / 25);

        mAdapter = new IndicatorAdapter(getBaseActivity(), itemWidth);
        // rvIndicator.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        for (int i = 0; i < 25; i++) {
            mAdapter.addData(new EchoStatus(0, "Test"));
        }
        rvIndicator.setAdapter(mAdapter);
    }

    @Override
    protected void initializeBehavior() {
        int size = ScreeUtils.INSTANCE.getScreenWidth(getBaseActivity()) -
                getResources().getDimensionPixelSize(R.dimen._220sdp);
        int itemWidth = (size / 25);
        setdata(itemWidth);
        calculateCoins();
        //File path = getActivity().getExternalFilesDir("");
        File path = new File(CouldcoinApplication.CloudcoinHome);

        if (path == null) {
            Log.e("TAG", "Failed to get External directory");
            return;
        }
        bankDirPath = CommonUtils.createDirectory(path, BANK_DIR_NAME, DIR_BASE);
        limboDirPath = CommonUtils.createDirectory(path, BANK_LIMBO_NAME, DIR_BASE);
        counterfeitPath = CommonUtils.createDirectory(path, COUNTERFEIT_DIR_NAME, DIR_BASE);
        importPath = CommonUtils.createDirectory(path, IMPORT_DIR_NAME, DIR_BASE);
        frackedDirPath = CommonUtils.createDirectory(path, FRACKED_DIR_NAME, DIR_BASE);
        trashPath = CommonUtils.createDirectory(path, TRASH_DIR_NAME, DIR_BASE);
        exportPath = CommonUtils.createDirectory(path, EXPORT_DIR_NAME, DIR_BASE);
    }

    private void calculateCoins() {
        int bankCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Bank"),".bin");
        int frackedCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Fracked"),".bin");
        totalCoins = bankCoins + frackedCoins;
        totalPrice.setText(String.valueOf(bankCoins + frackedCoins));
    }

    private void setdata(int itemWidth) {
        mhandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setupEchoData(itemWidth);
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mhandler.postDelayed(this, 1000);
                    }
                });
            }

        }).start();
    }

    private void setupEchoData(int itemWidth) {
        try {
            RAIDA raida = RAIDA.getInstance();
            ArrayList<RaidaItems> raidalist = Constants.RAIDALISTS;
            raida.setRaidaList(raidalist);
            raida.setCallbacks(this);
            raida.echo();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String copyFile(Uri path, String importPath) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        String fileName = CommonUtils.getFileName(getBaseActivity(), path);
        File file = new File(importPath, System.currentTimeMillis() + fileName);
        try {
            InputStream inputStream = getBaseActivity().getContentResolver().openInputStream(path);
            int originalSize = inputStream.available();
            bis = new BufferedInputStream(inputStream);
            bos = new BufferedOutputStream(new FileOutputStream(
                    file, false));
            byte[] buf = new byte[originalSize];
            bis.read(buf);
            do {
                bos.write(buf);
            } while (bis.read(buf) != -1);

            bos.flush();
            bos.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return file.getPath();
    }


    @Override
    public void ReportBack(byte[] lMsg, String hex, int commandCode, int raidaID, int passcount) {
        Log.d("echo response", String.valueOf(raidaID));
        char[] ch = new char[hex.length()];
        for (int i = 0; i < hex.length(); i++) {
            ch[i] = hex.charAt(i);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ch[4]).append(ch[5]);
        String stats = sb.toString();
        Log.d("echo res", stats);
        EchoStatus status1;
        if (stats.equals("FA")) {
            status1 = new EchoStatus(raidaID, "success");


        } else {
            status1 = new EchoStatus(raidaID, "failure");
        }
        mAdapter.replaceData(status1, raidaID);
        rvIndicator.post(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void ReportBackError(Exception e, byte[] data, int commandCode, int raidaID) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fragment_withdraw_ivBack:
                getFragmentManager().popBackStack();
                break;
            case R.id.fragment_withdraw_llExport:
                try {
                    llProgress.setVisibility(View.VISIBLE);
                    handleWithdrawalTask();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleWithdrawalTask() throws Exception {



        String withdrawAmount = etWithdraw.getText().toString();
        int amount = 0;
        if (!withdrawAmount.equals("")) {
            amount = Integer.parseInt(withdrawAmount);
            if (amount > totalCoins) {
                changeViewStatus(error_message);
            } else {
                savedCoins.clear();
                int finalAmount = amount;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            exportCoins(finalAmount);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
        } else {
            changeViewStatus(error_no_amount);
        }
    }

    private void changeViewStatus(String error) {
        errormessage.setText(error);
        errormessage.setVisibility(View.VISIBLE);
        tvExport.setText("Cancel");
        etWithdraw.setEnabled(false);
        llWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvExport.setText("Export");
                errormessage.setVisibility(View.GONE);
                etWithdraw.setText("");
                etWithdraw.setEnabled(true);
                tvExport.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(View v) {
                        try {
                            llProgress.setVisibility(View.VISIBLE);
                            handleWithdrawalTask();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void exportCoins(int amount) throws Exception {
        int requestedCount = amount;
        ArrayList<String> exportFiles = new ArrayList<>();
        int authCoinCount = CommonUtils.getCoinCount(bankDirPath);
        int frackedCoinCount = CommonUtils.getCoinCount(frackedDirPath);
        int limboCoinCount = CommonUtils.getCoinCount(limboDirPath);
        int counterfeitcount = CommonUtils.getCoinCount(counterfeitPath);
        for (int i = 0; i < amount; i++) {
            if (requestedCount > 0) {
                if (authCoinCount > 0) {
                    File file = new File(bankDirPath);
                    File[] list = file.listFiles();
                    String address = copyFile(Uri.fromFile(list[0]), exportPath);
                    exportFiles.add(address);
                    updateData(address);
                    deleteCoin(list[0].getPath());
                    coinsToDelete.add(Uri.parse(address));
                    requestedCount--;
                    authCoinCount--;

                } else {
                    if (frackedCoinCount > 0) {
                        File file = new File(frackedDirPath);
                        File[] list = file.listFiles();
                        String address = "";
                        address = copyFile(Uri.fromFile(list[0]), exportPath);
                        exportFiles.add(address);
                        deleteCoin(list[0].getPath());
                        coinsToDelete.add(Uri.parse(address));
                        updateData(address);
                        requestedCount--;
                        frackedCoinCount--;
                    } else if (limboCoinCount > 0) {
                        File file = new File(limboDirPath);
                        File[] list = file.listFiles();
                        String address = "";
                        address = copyFile(Uri.fromFile(list[0]), exportPath);
                        exportFiles.add(address);
                        deleteCoin(list[0].getPath());
                        coinsToDelete.add(Uri.parse(address));
                        updateData(address);
                        requestedCount--;
                        limboCoinCount--;
                    } else if (counterfeitcount > 0) {
                        File file = new File(counterfeitPath);
                        File[] list = file.listFiles();
                        String address = "";
                        address = copyFile(Uri.fromFile(list[0]), exportPath);
                        exportFiles.add(address);
                        deleteCoin(list[0].getPath());
                        coinsToDelete.add(Uri.parse(address));
                        updateData(address);
                        requestedCount--;
                        counterfeitcount--;
                    } else {
                        showMessage("Server Error Please try again");
                    }
                }
                showMessage(amount + "  Coins exported to Exported Folder");
            } else {
                showMessage("count not available");
            }
        }

        String exportedPath=cretaPNGfromExportedFiles(exportPath,exportFiles);
        saveTransaction(savedCoins, amount);
        updateView();

        openShareDialog(exportedPath);
    }

    private void openFile(String exportPath) {
        String path="/storage/emulated/0/CloudCoins/Export/";
       // String rootPath = "content://storage/emulated/0/Clo"
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri mydir = Uri.parse("content://"+path);
        intent.setDataAndType(mydir, "*/*");
        startActivity(intent);
    }

    private void showMessage(String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseActivity(), message, Toast.LENGTH_LONG).show();
            }
        });

    }

    private void updateView() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                llProgress.setVisibility(View.GONE);
                etWithdraw.setText("");
                etMemo.setText("");
                calculateCoins();
            }
        });

    }

    public static InputStream bitmapToInputStream(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        System.out.println("........length......" + imageInByte);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);
        return bis;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("UseCompatLoadingForDrawables")
    public String cretaPNGfromExportedFiles(String mexportPath, ArrayList<String> exportedFiles)
    {

        ArrayList<byte[]> allCoins = new ArrayList<>();
        for(int i=0;i<exportedFiles.size();i++){
            String fileName = exportedFiles.get(i);
            byte[] bytes = KotlinUtils.INSTANCE.readBinaryFile(fileName);
            allCoins.add(bytes);
        }
        try {
            String destination = mexportPath +"/"+ String.valueOf(exportedFiles.size()).concat(".Cloudcoin.") + System.currentTimeMillis() + ".png";
            Log.d("path to write", destination);
            Bitmap bitmap=addTextToImage(exportedFiles.size());
            InputStream inputStream = bitmapToInputStream(bitmap);
            RAIDA.getInstance().coinToPng(inputStream, destination, allCoins);

            for(int i=0;i<coinsToDelete.size();i++){
                deleteCoin(coinsToDelete.get(i).getPath());
            }
            return destination;
        }
        catch(Exception e){
            e.printStackTrace();
    }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Bitmap addTextToImage(int size) {
        String amount= NumberWordConverter.convert(size);
       try {
           Bitmap bmp = drawTextToBitmap(amount, size);
           int height=bmp.getHeight();
           int width=bmp.getWidth();
           Log.d("bitmap height1",String.valueOf(height));
           Log.d("bitmap width1",String.valueOf(width));
           return bmp;
       }
       catch(Exception e){
             e.printStackTrace();
        }
       return null;
    }
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public Bitmap drawTextToBitmap( String mText,int size) {
        try {
            Bitmap resizedmap = BitmapFactory.decodeResource(getResources(),R.raw.base_image);
            Bitmap bitmap=getResizedBitmap(resizedmap,375,667);
            android.graphics.Bitmap.Config bitmapConfig =   bitmap.getConfig();
            int height=bitmap.getHeight();
            int width=bitmap.getWidth();
            Log.d("bitmap height3",String.valueOf(height));
            Log.d("bitmap width3",String.valueOf(width));
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }
            bitmap = bitmap.copy(bitmapConfig, true);
            int height2=bitmap.getHeight();
            int width2=bitmap.getWidth();
            Log.d("bitmap height2",String.valueOf(height2));
            Log.d("bitmap width2",String.valueOf(width2));
            Typeface typeface = ResourcesCompat.getFont(getBaseActivity(), R.font.barlow_regular);
            Typeface typeface_bold = ResourcesCompat.getFont(getBaseActivity(), R.font.barlow_bold);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTypeface(typeface);
            paint.setTextSize(16);

            /*------Adding Amount in Words -------*/
            Rect bounds = new Rect();
            paint.getTextBounds(mText, 0, mText.length(), bounds);
            int x = 50;
            int y = 340;
            canvas.drawText(mText, x, y, paint);

            /*------Adding Date -------*/
            Rect bound = new Rect();
            paint.getTextBounds(mText, 0, mText.length(), bound);
            int x1 = 50;
            int y1 = 375;
            String timestamp=getCurrentDateTime();
            canvas.drawText(timestamp, x1, y1, paint);


            /*----Adding Amount in Digit------*/
            Rect bound2 = new Rect();
            paint.getTextBounds(mText, 0, mText.length(), bound2);
            int x2 = 20;
            int y2 = 500;
            String size_word=formatSize(size);
            int font_size=getFontSize(size_word.length());
            paint.setTextSize(font_size);
            paint.setTypeface(typeface_bold);
            canvas.drawText(size_word.toUpperCase(Locale.ROOT), x2, y2, paint);

            /*----Adding Cloudcoin Title ------*/
            //int screensize = ScreeUtils.INSTANCE.getScreenWidth(getBaseActivity());
            String text="Cloudcoin";
            Rect bound3 = new Rect();
            paint.getTextBounds(text, 0, text.length(), bound3);
            int x3 = 210;
            int y3 = 50;

            paint.setTextSize(30);
            paint.setTypeface(typeface_bold);
            paint.setColor(getResources().getColor(R.color.colorExportBlue));
            canvas.drawText(text, x3, y3, paint);

            /*----Adding Cloudcoin Bottom Text ------*/
            String bottom="More info on Cloudcoin.global";
            Rect bound6 = new Rect();
            paint.getTextBounds(bottom, 0, bottom.length(), bound6);
            int x6 = 10;
            int y6 = 650;
            paint.setTypeface(typeface);
            paint.setTextSize(13);
            paint.setColor(getResources().getColor(R.color.colorExportBlue));
            paint.setTypeface(typeface_bold);
            canvas.drawText(bottom, x6, y6, paint);



            /*----Adding Cloudcoin Bottom Text advise ------*/
            String bottomt="Upload this File to your Skyvault";
            String bottomtext="or POWN it or keep it wherever you want";
            Rect bound7 = new Rect();
            paint.getTextBounds(bottomt, 0, bottomt.length(), bound7);
            int x7 = 10;
            int y7 = 580;
            paint.setTextSize(17);
            paint.setTypeface(typeface);
            paint.setColor(Color.WHITE);
            canvas.drawText(bottomt, x7, y7, paint);

            Rect bound8 = new Rect();
            paint.getTextBounds(bottomtext, 0, bottomtext.length(), bound8);
            int x8 = 10;
            int y8 = 600;
            paint.setTextSize(17);
            paint.setColor(Color.WHITE);
            canvas.drawText(bottomtext, x8, y8, paint);

            /*----Adding Cloudcoin Title Vertical ------*/
            String vText="CC";
            Rect bound4 = new Rect();
            paint.getTextBounds(vText, 0, vText.length(), bound4);
            int x4 = 20;
            int y4 = 10;
            paint.setTextSize(35);
            paint.setColor(getResources().getColor(R.color.colorExportBlue));
            paint.setTypeface(typeface_bold);
            canvas.save();
            canvas.rotate(90f, 10, 10);
            canvas.drawText(vText, x4, y4, paint);
            canvas.restore();


            /*----Adding Amount in Digit Vertical------*/
            Rect bound5 = new Rect();
            paint.getTextBounds(mText, 0, vText.length(), bound5);
            int x5 = 80;
            int y5 = 10;
            paint.setTextSize(35);
            paint.setColor(Color.WHITE);
            canvas.save();
            canvas.rotate(90f, 10, 10);
            canvas.drawText(String.valueOf(size), x5, y5, paint);
            canvas.restore();

            return bitmap;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }

    private int getFontSize(int num) {
        int  fontsize=0;
        if (num <= 4) {
            fontsize = 122;

        } else if (num <= 5) {
            fontsize = 98;
        } else if (num <= 6) {
            fontsize = 83;
        } else if (num <= 7) {
            fontsize = 68;
        } else if (num <= 8) {
            fontsize = 60;
        } else if (num <= 9) {
            fontsize = 54;
        } else if (num <= 10) {
            fontsize = 46;
        }
   return fontsize;

    }

    private String formatSize(int size) {
      String size_word="";

        if(size<11)
            size_word=NumberWordConverter.convert(size);
        else if(size<1000)
            size_word=String.valueOf(size);
        else if (size < 1000000) {
            if (size % 100 == 0) {
                size = size / 1000;
                size_word = String.valueOf(size) + "K";
            } else {
                size_word=getFormattednumberString(size);
            }

        } else if (size < 1000000000) {
            if (size % 100000 == 0) {
                size = size / 1000;
                size_word = String.valueOf(size) + "M";
            } else {
                size_word=getFormattednumberString(size);
            }
        } else {
            if (size % 100000 == 0) {
                size = size / 1000;
                size_word = String.valueOf(size) + "B";
            } else {
                size_word=getFormattednumberString(size);
            }
        }
        return size_word;
    }

    private String getFormattednumberString(int size) {
        DecimalFormat formatter = new DecimalFormat("#,###,###");
        String sFormattedNumber = formatter.format(size);
        return sFormattedNumber;
    }


    private String getCurrentDateTime() {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
        return dateFormat.format(date).toString();
    }


    private void openShareDialog(String logPath) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File infile=new File(logPath);
                Uri uri = FileProvider.getUriForFile(getBaseActivity(), "com.cloudcoin2.wallet", infile);

                Intent intent = ShareCompat.IntentBuilder.from(getBaseActivity())
                        .setStream(uri) // uri from FileProvider
                        .setType("text/html")
                        .getIntent()
                        .setAction(Intent.ACTION_SEND) //Change if needed
                        .setDataAndType(uri, "*/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(intent);
            }
        });

    }


    private void updateData(String address) {
        File inFile = new File(address);
        String extension = CommonUtils.getFileExtension(inFile.getName()).toLowerCase();
        Log.d("extension", extension);
        byte[] rawdata = KotlinUtils.INSTANCE.readBinaryFile(inFile.getPath());
        try {
            List<CloudCoin> cc = RAIDA.getInstance().binaryToCoins(rawdata);
            byte[] serial = cc.get(0).getSerial();
            updateDatabase(serial);
            Log.d("serial", String.valueOf(new BigInteger(serial).intValue()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTransaction(ArrayList<SavedCoin> savedCoins, int mImportAmount) {
        Transactions task = new Transactions();
        //  task.setId(id);
        task.setDate(String.valueOf(System.currentTimeMillis()));
        //               task.setMemo(memo);
        task.setAmount(String.valueOf(mImportAmount));
        task.setMemo(etMemo.getText().toString());
        task.setType("1");

        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .transactionDao()
                .insertAll(task);


    }

    private void updateDatabase(byte[] serial) {

        Log.d("serial in db ", String.valueOf(new BigInteger(serial).intValue()));
        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .upDateCoinStatus(new BigInteger(serial).intValue(), "Exported");

    }

    public void deleteCoin(String path) throws Exception {
        boolean deleted = false;

        File f = new File(path);
        try {
            f.delete();
        } catch (Exception e) {
            Log.e("Failed to delete coin ", path);
            e.printStackTrace();
            throw new Exception("Failed to delete coin " + path + " " + e.getMessage());
        }
    }


}
