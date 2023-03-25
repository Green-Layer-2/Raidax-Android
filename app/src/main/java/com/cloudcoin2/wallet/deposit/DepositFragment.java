package com.cloudcoin2.wallet.deposit;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.Adapter.IndicatorAdapter;
import com.cloudcoin2.wallet.BottomSheet.BottomSheetInterface;
import com.cloudcoin2.wallet.BottomSheet.PownSummaryBottomSheet;
import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.Model.IncomeFile;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.Utils.CommandCodes;
import com.cloudcoin2.wallet.Utils.CommonUtils;
import com.cloudcoin2.wallet.Utils.Constants;
import com.cloudcoin2.wallet.Utils.CouldcoinApplication;
import com.cloudcoin2.wallet.Utils.EchoResult;
import com.cloudcoin2.wallet.Utils.KotlinUtils;
import com.cloudcoin2.wallet.Utils.RAIDA;
import com.cloudcoin2.wallet.Utils.RAIDAX;
import com.cloudcoin2.wallet.Utils.ScreeUtils;
import com.cloudcoin2.wallet.Utils.UDPCallBackInterface;
import com.cloudcoin2.wallet.base.BaseFragment2;
import com.cloudcoin2.wallet.db.AppDatabase;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.SavedCoin;
import com.cloudcoin2.wallet.db.Transactions;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunk;

/**
 * Created by Arka Chakraborty on 15/02/22
 */
public class DepositFragment extends BaseFragment2 implements View.OnClickListener, UDPCallBackInterface {
    private RAIDAX raidax = RAIDAX.getInstance();

    private ArrayList<EchoStatus> mList = new ArrayList<>();
    private IndicatorAdapter mAdapter;
    private RecyclerView rvIndicator;
    private TextView tvUpload, tvDeposit, tvCancel;
    private EditText etMemo;
    private EditText txtLockerCode;
    private LinearLayout llProgress;
    private ImageView ivBack;
    private ImageView ivRefresh;
    static String BANK_DIR_NAME = "Bank";
    static String BANK_LIMBO_NAME = "Limbo";
    static String COUNTERFEIT_DIR_NAME = "Counterfeit";
    static String FRACKED_DIR_NAME = "Fracked";
    static String IMPORT_DIR_NAME = "Import";
    static String TRASH_DIR_NAME = "Trash";
    static String DIR_BASE = "CloudCoins";
    private int totalApiCallCount = 0;
    private int importAmount = 0;
    private int counterfeitAmount = 0;
    private int limboAmount = 0;

    private int importProcessed = 0;
    private int totalDetectedCoinCount = 1;
    private int echoCalled = 0;
    private int echoPassed = 0;
    private int fixCounter = 0;
    private int callFixcounter = 0;
    private int fixLostRequest = 0;
    private int fixLostResponse = 0;
    private boolean isDeposited = false;
    int passCount, frackedCount, cFeitCount, lostCount;
    private ArrayList<Uri> coinsToDelete = new ArrayList<>();
    private ArrayList<Integer> fixedServers = new ArrayList<>();
    private ArrayList<Integer> pownResponses = new ArrayList<>();
    Handler mhandler;
    String mPownStatus = "";
    int mResponseCount = 0, sDuplicateCount = 0;
    private AppDatabase database;
    ArrayList<SavedCoin> savedCoins = new ArrayList<SavedCoin>();
    BottomSheetInterface bottomSheetInterface;
    ArrayList<RaidaItems> raidaLists;
    ArrayList<String> files;
    private ArrayList<IncomeFile> loadedIncome = new ArrayList<IncomeFile>();
    ConstraintLayout layout;
    RAIDA raida = RAIDA.getInstance();

    private String bankDirPath, limboDirPath, counterfeitPath, importPath, frackedDirPath, trashPath;
    final static int REQUEST_CODE_IMPORT_DIR = 1;

    @Override
    protected int defineLayoutResource() {
        return R.layout.fragment_deposit;
    }

    @Override
    protected void initializeComponent(@NonNull View view) {
        rvIndicator = view.findViewById(R.id.fragment_deposit_rvIndicator);
        layout = view.findViewById(R.id.constraint);
        tvDeposit = view.findViewById(R.id.fragment_deposit_tvDeposit);
        txtLockerCode = view.findViewById(R.id.txtLockerCode);
        tvDeposit.setOnClickListener(this);
        tvCancel = view.findViewById(R.id.fragment_deposit_tvCancel);
        tvCancel.setOnClickListener(this);

        llProgress = view.findViewById(R.id.fragment_deposit_llProgress);
        llProgress.setVisibility(View.GONE);
        ivBack = view.findViewById(R.id.fragment_deposit_ivBack);
        ivBack.setOnClickListener(this);
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
        //setdata(itemWidth);
        files = null;
        //File path = getActivity().getExternalFilesDir("");
        File path = new File(CouldcoinApplication.CloudcoinHome );

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    echo();
                }catch (Exception e) {
                    Log.e("RAIDAX", e.getMessage());
                }
            }
        }).start();

        fetchTask();


        Log.e("TAG", "Fetched all");
    }


    private void fetchTask() {
        class SaveTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                final List<SavedCoin> ss = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .coinDao()
                        .getAllBank("Bank", "Fracked");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }
        SaveTask st = new SaveTask();
        st.execute();
    }

    private void echo() {
        try {
            raidax.execute(CommandCodes.Echo);
            EchoResult result = raidax.getEchoResult();
            int i =0;
            String status  = "failure";
            for (String res: result.getResponseCodes()) {
                if(res.equals("FA")) status = "success";
                else status = "failure";
                mAdapter.replaceData(new EchoStatus(i,status),i);
                i++;
            }

            rvIndicator.post(new Runnable() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });

            //setupListeners(true);

        }
        catch (Exception e) {

        }

    }


    public void importFromLocker() {
        String lockerCode = txtLockerCode.getText().toString();

        if(lockerCode.length() == 0) {
            Toast.makeText(getActivity(),"Please enter a valid locker code.",Toast.LENGTH_SHORT).show();
            return;
        }

        llProgress.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("Locker", lockerCode);
                try {
                    raidax.importLockerCode(lockerCode);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            llProgress.setVisibility(View.GONE);
                            Toast.makeText(getActivity(), "Locker Import operation completed", Toast.LENGTH_SHORT).show();
                            // Perform UI-related operations here
                        }
                    });
                }catch (Exception e) {
                    Log.e("Lokcer", e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fragment_deposit_tvCancel:
                if (files != null && files.size() > 0) {
                    files.clear();
                    if (loadedIncome != null && loadedIncome.size() > 0)
                        loadedIncome.clear();
                    tvUpload.setText("Select Cloud Coins");
                    tvCancel.setVisibility(View.GONE);
                }
                break;
            case R.id.fragment_deposit_tvDeposit:
                importFromLocker();
                break;
            case R.id.fragment_deposit_ivBack:
                //noinspection deprecation
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
                break;

            default:
                break;
        }
    }

    private void updateView() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvUpload.setText("Select Cloud Coins");
                tvCancel.setVisibility(View.GONE);
                llProgress.setVisibility(View.GONE);
            }
        });


    }

    private void performDBtask() {
        //savedCoins.clear();
        passCount = 0;
        lostCount = 0;
        cFeitCount = 0;
        frackedCount = 0;

        Log.d("savedcoin size", String.valueOf(savedCoins.size()));
        for (int i = 0; i < savedCoins.size(); i++) {
            if (savedCoins.get(i).getStatus().equals("Bank"))
                passCount++;
            else if (savedCoins.get(i).getStatus().equals("Fracked"))
                passCount++;

            else if (savedCoins.get(i).getStatus().equals("Counterfeit"))
                cFeitCount++;
            else if (savedCoins.get(i).getStatus().equals("Lost/Limbo"))
                lostCount++;
        }
        saveTransaction(savedCoins, passCount);
        for (int i = 0; i < coinsToDelete.size(); i++) {
            try {
                deleteCoin(coinsToDelete.get(i).getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateView();
        if (savedCoins.size() > 0)
            openBottomSheet(savedCoins.size(), passCount, lostCount, cFeitCount, sDuplicateCount);
        else
            showError("All the Coins Already Imported");

    }

    private void pownCoins(ArrayList<IncomeFile> loadedIncome) {
        performPowning(loadedIncome, 1);
        //performDBtask();

    }

    private void openBottomSheet(int size, int passCount, int lostCount, int cFeitCount, int sDuplicateCount) {
        bottomSheetInterface = new BottomSheetInterface() {
            @Override
            public void Onclick() {
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
            }
        };
        PownSummaryBottomSheet.newInstance(getFragmentManager(), size, passCount, cFeitCount, lostCount, bottomSheetInterface, sDuplicateCount);
    }

    private void saveTransaction(ArrayList<SavedCoin> savedCoins, int mImportAmount) {
        //Coin adding to database
        if (savedCoins.size() > 0) {
            DatabaseClient.getInstance(getActivity()).getAppDatabase()
                    .coinDao()
                    .insertAll(savedCoins);
        }
        //Saving Transaction to Database
        if (mImportAmount > 0) {
            Transactions task = new Transactions();
            task.setDate(String.valueOf(System.currentTimeMillis()));
            task.setAmount(String.valueOf(mImportAmount));
            task.setMemo(etMemo.getText().toString());
            task.setType("0");

            DatabaseClient.getInstance(getActivity()).getAppDatabase()
                    .transactionDao()
                    .insertAll(task);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void ReportBack(byte[] lMsg, String hex, int commandCode, int raidaID, int sPassCount) {
        Log.e("RAIDA", "Command Code:" + commandCode);

        if (commandCode == 5) {
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
        } else if (commandCode == 0) {
            mResponseCount++;
            Log.d("response", String.valueOf(mResponseCount));
            if (mResponseCount == 25) {

                mResponseCount = 0;
                processPownResponse();

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processPownResponse() {

        List<CloudCoin> coins = RAIDA.getInstance().getCloudCoins();
        Log.d("POWN", "POWN result for " + coins.size() + " file(s)");
        savedCoins.clear();

        for (int i = 0; i < coins.size(); i++) {
            Log.d("POWN", "COIN:" + RAIDA.bytesToHex(coins.get(i).getSerial()) + "");
            if (coins.get(i).getPassCount() == 25) {
                Log.d("POWN", "Coin " + coins.get(i).getFileName() + " is fully authentic");
                saveCoin(coins.get(i), "Bank");
                updateDatabase(coins.get(i).getSerial(), "Bank");
                passCount++;
                // saveFile(coins.get(i));
            } else if (coins.get(i).getPassCount() >= 16) {
                Log.d("POWN", "Coin " + coins.get(i).getFileName() + " is fracked at " + coins.get(i)
                        .getFailCount() + " places, no response from " + coins.get(i).getNoResponseCount() + " raidas");
                saveCoin(coins.get(i), "Fracked");
                updateDatabase(coins.get(i).getSerial(), "Fracked");
                passCount++;
                //  saveFile(coins.get(i));
            } else if (coins.get(i).getFailCount() > 9) {
                Log.d("POWN", "Coin " + coins.get(i).getFileName() + " is counterfeit because "
                        + coins.get(i).getFailCount() + " raidas returned allfail");
                saveCoin(coins.get(i), "Counterfeit");
                updateDatabase(coins.get(i).getSerial(), "Counterfeit");
                cFeitCount++;
            } else {
                Log.d("POWN", "Coin " + coins.get(i).getFileName() + " is lost/in limbo and " +
                        "needs to be found because - pass: " + coins.get(i).getPassCount() + ", fail: " +
                        coins.get(i).getFailCount() + ", no response: " + coins.get(i).getNoResponseCount());
                saveCoin(coins.get(i), "Lost/Limbo");
                updateDatabase(coins.get(i).getSerial(), "Lost/Limbo");
                lostCount++;
            }

        }
        performDBtask();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void ReportBackError(Exception e, byte[] data, int commandCode, int raidaID) {
        mResponseCount++;
        if (mResponseCount == 25) {
            mResponseCount = 0;
            if (commandCode == 0) {
                processPownResponse();
            }

        }
    }

    private void updateDatabase(byte[] serial, String pStatus) {
        SavedCoin coin = new SavedCoin();
        coin.setAmount("1");
        coin.setSerialNumber(new BigInteger(serial).intValue());
        coin.setDenomination("1");
        coin.setStatus(pStatus);
        savedCoins.add(coin);
        checkandDeletefromDB(serial);

    }

    private void checkandDeletefromDB(byte[] serial) {

        List<SavedCoin> coins = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .getAll();
        int ss = new BigInteger(serial).intValue();
        for (int i = 0; i < coins.size(); i++) {
            int serialno = coins.get(i).getSerialNumber();
            if (serialno == ss)
                DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .coinDao()
                        .deleteCoin(serialno);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void loadIncomeFromFiles(ArrayList<String> files) {
        llProgress.setVisibility(View.VISIBLE);
        String extension;
        int fileType;
        loadedIncome.clear();
        for (String file : files) {
            File inFile = new File(file);
            try {
                if (inFile.isFile()) {
                    extension = CommonUtils.getFileExtension(inFile.getName()).toLowerCase();
                    if (extension.equals("png")) {
                        fileType = IncomeFile.TYPE_PNG;
                        // Toast.makeText(getBaseActivity(), String.valueOf(fileType), Toast.LENGTH_LONG).show();

                       // String[] pngPath = storeBinaryData(rawPngData, inFile.getAbsolutePath());
                       // for (String s : pngPath) {
                            loadedIncome.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
                         //   coinsToDelete.add(Uri.fromFile(new File(s)));
                       // }
                    } else {
                        fileType = IncomeFile.TYPE_STACK;
                        loadedIncome.add(new IncomeFile(inFile.getAbsolutePath(), fileType));
                    }

                }
            } catch (Exception e) {
                showError("Failed to read file " + file);
                Log.e("TAG", "Failed to read file " + file);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String[] storeBinaryData(byte[] rawPngData, String absolutePath) throws FileNotFoundException {

        int length = rawPngData.length / 448;
        String[] pathList = new String[length];
        int index = 0;
        int counter = 0;
        for (int i = 0; i < length; i++) {
            byte[] pngdata = new byte[448];
            for (int j = 0; j < 448; j++) {
                pngdata[j] = rawPngData[counter];
                counter++;
            }
            String savePath = saveFiletoPath(pngdata, String.valueOf(i));
            Log.d("savedpath", savePath);
            pathList[index] = savePath;
            index++;

        }
        return pathList;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String saveFiletoPath(byte[] pngdata, String absolutePath) throws FileNotFoundException {
        File f = new File(importPath, absolutePath);
        Path path = Paths.get(f.getAbsolutePath());
        try {
            Files.write(path, pngdata);
        } catch (IOException ex) {

        }
        return path.toString();
    }


    private byte[] readChunkFromPng(String path) {
        byte[] mPngArray = new byte[0];
        try (PngReader pngr = new PngReader(new File(path))) {
            pngr.readSkippingAllRows(); // reads only metadata
            for (PngChunk c : pngr.getChunksList().getChunks()) {
                if (!ChunkHelper.isText(c)) {
                    String cid = c.id;
                    Log.d("key2", c.id);
                    if (cid.equals("cLDc")) {
                        ChunkRaw chunk = c.getRaw();
                        mPngArray = chunk.data;
                        Log.d("rawchunk", Arrays.toString(mPngArray));
                    }
                }
            }
            pngr.end();
        }
        return mPngArray;

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveCoin(CloudCoin coin, String status) {
        try {
            byte[] coinData = RAIDA.getInstance().coinToBinary(coin);
            String filename = coin.getFileName();
            String path = "";
            if (status.equals("Bank"))
                path = bankDirPath + "/" + filename;
            else if (status.equals("Fracked"))
                path = frackedDirPath + "/" + filename;
            else if (status.equals("Counterfeit"))
                path = counterfeitPath + "/" + filename;
            else if (status.equals("Lost/Limbo"))
                path = limboDirPath + "/" + filename;
            File file = new File(path);
            Path destpath = Paths.get(file.getAbsolutePath());
            Files.write(destpath, coinData);

            Log.d("Saved in ", destpath.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void performPowning(ArrayList<IncomeFile> Income, int id) {
        RAIDA raida = RAIDA.getInstance();
        int duplicateCount = 0;
        raida.setCallbacks(this);
        //raida.setDebug(true);
        ArrayList<CloudCoin> ccoins = new ArrayList<>();
        for (int i = 0; i < Income.size(); i++) {

            String loadedIncome = Income.get(i).fileName;
            byte[] binary;
            if(Income.get(i).fileType==IncomeFile.TYPE_PNG)
            {
                binary = readChunkFromPng(Income.get(i).fileName);
            }
            else
            {
                binary = KotlinUtils.INSTANCE.readBinaryFile(loadedIncome);
            }
            if(binary!=null)
            {
                try{
                    List<CloudCoin> cc = raida.binaryToCoins(binary);

                    for(int coinCounter =0; coinCounter<cc.size(); coinCounter++)
                    {
                        byte[] serial = cc.get(coinCounter).getSerial();
                        Log.d("POWN", "Serial:"+Arrays.toString(serial));
                        mResponseCount = 0;
                        boolean alreadyInBank = checkDB(cc.get(coinCounter));
                        if (!alreadyInBank || !coinFileExists(serial)) {
                            clearCoinFromFile(serial);
                            ccoins.add(cc.get(coinCounter));
                            isDeposited = true;
                        }
                       else
                        {
                            duplicateCount++;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    duplicateCount++;
                }
            }

        }
        sDuplicateCount = duplicateCount;
        try {
            Log.d("POWN","Calling POWN service to pown "+ccoins.size()+" Coins");
            if(ccoins.size()>0)
                raida.pown(ccoins);
            else
            {
                updateView();
                updateView();
                showError("No valid coins to pown");
            }
            try {
                Thread.sleep(1000);//
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("POWN", "Complete");
        } catch (Exception ex) {
            Log.d("POWN", "EXCEPTION: " + ex.getMessage());
            ex.printStackTrace();
            updateView();
            showError("EXCEPTION: " + ex.getMessage());
        }

    }

    private boolean coinFileExists(byte[] serial) {
        int serialn = new BigInteger(serial).intValue();
        String status = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .getCoinStatus(serialn);
        String dirpath = "";
        if (status.equals("Bank"))
            dirpath = bankDirPath;
        else if (status.equals("Fracked"))
            dirpath = frackedDirPath;
        else if (status.equals("Counterfeit"))
            dirpath = counterfeitPath;
        else
            dirpath = limboDirPath;


        dirpath = dirpath+"/1.CloudCoin.1."+serialn+".bin";
        Log.d("POWN", "Checking if exists:"+dirpath);
        File file = new File(dirpath);
        if(!file.exists())
            return false;

        try {
            byte[] binary = KotlinUtils.INSTANCE.readBinaryFile(dirpath);
            List<CloudCoin> cc = RAIDA.getInstance().binaryToCoins(binary);
            int fSerial = new BigInteger(cc.get(0).getSerial()).intValue();
            if (fSerial == serialn)
               return true;
        } catch (Exception e) {
            return false;
        }
        return false;
    }


    private void clearCoinFromFile(byte[] serial) {
        int serialn = new BigInteger(serial).intValue();
        String status = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .getCoinStatus(serialn);
        String dirpath = "";
        if(status==null)
            return;
        if (status.equals("Bank"))
            dirpath = bankDirPath;
        else if (status.equals("Fracked"))
            dirpath = frackedDirPath;
        else if (status.equals("Counterfeit"))
            dirpath = counterfeitPath;
        else
            dirpath = limboDirPath;

        dirpath = dirpath+"/1.CloudCoin.1."+serialn+".bin";
        Log.d("POWN", "Checking if exists:"+dirpath);
        File file = new File(dirpath);
        if(file.exists())
            try {
                deleteCoin(dirpath);
            }catch (Exception e) {
                e.printStackTrace();
            }

    }

    private boolean checkDB(CloudCoin coin) {
        boolean isDepositedBefore = false;
        int inSerial = new BigInteger(coin.getSerial()).intValue();
        List<SavedCoin> coins = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .getValidCoin(inSerial);

        for (int i = 0; i < coins.size(); i++) {
            int sSerial = coins.get(i).getSerialNumber();
            if (inSerial == sSerial) {
                isDepositedBefore = true;
                return isDepositedBefore;
            }
        }
        return isDepositedBefore;

    }
    public void deleteCoin(String path) throws Exception {

        String trashPath = path.replace("Bank", "Trash");
        trashPath = trashPath.replace("Fracked", "Trash");
        File file = new File(trashPath);
        if(file.exists())
            moveCoin(trashPath, System.currentTimeMillis()+"."+trashPath);
        moveCoin(path, trashPath);
    }

    private void moveCoin(String fromCoin, String toCoin) {

        InputStream in = null;
        OutputStream out = null;
        try {


            in = new FileInputStream(fromCoin);
            out = new FileOutputStream(toCoin);

            byte[] buffer = new byte[448];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(fromCoin).delete();


        } catch (FileNotFoundException fnfe1) {
            Log.e("POWN", "File not found while moving coin:"+fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("POWN", "Exception while moving coin:"+e.getMessage());
        }

    }


    private void selectFile() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_IMPORT_DIR);
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

    @SuppressWarnings("deprecation")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ArrayList<String> loadedFiles = new ArrayList<>();
        int wrongCount = 0;
        if (requestCode == REQUEST_CODE_IMPORT_DIR) {
            if (data != null) {
                if (data.getClipData() != null) {

                    for (int index = 0; index < data.getClipData().getItemCount(); index++) {
                        Uri path = data.getClipData().getItemAt(index).getUri();
                        File infile = new File(String.valueOf(path));
                        Log.d("deposit", "file name:" + infile.getName());
                        String extension = CommonUtils.getFileExtension(infile.getName().toLowerCase(Locale.ROOT));
                        if (extension.equals("bin") || extension.equals("png") || extension.equals("")) {
                            String file = copyFile(path);

                            if (file != "") {
                                loadedFiles.add(file);
                            }
                        } else {
                            Log.d("deposit", "Extension:" + extension + " is not valid");
                            wrongCount++;
                        }
                    }
                } else {
                    Uri path = data.getData();
                    File infile = new File(String.valueOf(path));
                    String extension = CommonUtils.getFileExtension(infile.getName().toLowerCase(Locale.ROOT));
                    if (extension.equals("bin") || extension.equals("png") || extension.equals("")) {
                        String file = copyFile(path);
                        if (!file.equals("")) {
                            // coinsToDelete.add(path);
                            loadedFiles.add(file);
                        }
                    } else {
                        Log.d("deposit", "Extension:" + extension + " is not valid");
                        wrongCount++;
                    }
                }
            }
            if (resultCode == RESULT_OK) {
                importAmount = 0;
                if (loadedFiles.size() > 0) {

                    files = loadedFiles;
                    String text = files.size() + " file";
                    importAmount = files.size();
                    if (files.size() > 1) {
                        text = text + "s";
                    }
                    text = text + " Selected";
                    tvUpload.setText(text);
                    tvCancel.setVisibility(View.VISIBLE);


                }
                if (wrongCount > 0) {
                    showSnackbar(wrongCount, " Filesz not Imported as they are not valid");
                }

            } else {
                showError("No files Selected");
            }

            return;
        }

    }

    private void showSnackbar(int wrongCount, String message) {
        Snackbar snackbar = Snackbar
                .make(layout, String.valueOf(wrongCount).concat(message), Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.colorRed))
                .setTextColor(getResources().getColor(R.color.white))
                .setDuration(1800);

        snackbar.setActionTextColor(Color.RED);
        snackbar.show();
    }

    public String copyFile(Uri path) {
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
        coinsToDelete.add(Uri.fromFile(file));

        return file.getPath();
    }


    private void showError(String msg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAlert("Error", msg);
            }
        });
    }

    private void showAlert(String title, String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog1 = new AlertDialog.Builder(
                        getActivity()).create();
                alertDialog1.setTitle(title);
                alertDialog1.setMessage(message);
                alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                        // closed

                    }
                });
                alertDialog1.show();
            }
        });


    }
}
