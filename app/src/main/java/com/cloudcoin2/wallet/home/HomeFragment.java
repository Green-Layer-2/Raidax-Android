package com.cloudcoin2.wallet.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudcoin2.wallet.Adapter.IndicatorAdapter;
import com.cloudcoin2.wallet.Model.CloudCoin;
import com.cloudcoin2.wallet.Model.DetectionCoinsModel;
import com.cloudcoin2.wallet.Model.EchoStatus;
import com.cloudcoin2.wallet.Model.RaidaItems;
import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.Utils.CloudCoinFileWriter;
import com.cloudcoin2.wallet.Utils.CommandCodes;
import com.cloudcoin2.wallet.Utils.CommonUtils;
import com.cloudcoin2.wallet.Utils.Constants;
import com.cloudcoin2.wallet.Utils.CouldcoinApplication;
import com.cloudcoin2.wallet.Utils.Denominations;
import com.cloudcoin2.wallet.Utils.EchoResult;
import com.cloudcoin2.wallet.Utils.KotlinUtils;
import com.cloudcoin2.wallet.Utils.PermissionUtils;
import com.cloudcoin2.wallet.Utils.RAIDA;
import com.cloudcoin2.wallet.Utils.RAIDAX;
import com.cloudcoin2.wallet.Utils.UDPCallBackInterface;
import com.cloudcoin2.wallet.Utils.Utils;
import com.cloudcoin2.wallet.base.BaseFragment2;
import com.cloudcoin2.wallet.Utils.ScreeUtils;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.SavedCoin;
import com.cloudcoin2.wallet.db.Settings;
import com.cloudcoin2.wallet.db.Transactions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

/**
 * Created by Arka Chakraborty on 15/02/22
 * Last updated by Navraj Singh on 06/12/22
 * Removed manage all files permission request to comply with Google play store policy
 */
public class HomeFragment extends BaseFragment2 implements UDPCallBackInterface {

    private static String cloudcoinPath = "";
    private static final String MENU1 = "Fix Lost";
    private static final String MENU2 = "Health Check";
    private static final String MENU3 = "Fix Frack";
    static String BANK_DIR_NAME = "Bank";
    static String BANK_LIMBO_NAME = "Limbo";
    static String COUNTERFEIT_DIR_NAME = "Counterfeit";
    static String FRACKED_DIR_NAME = "Fracked";
    static String IMPORT_DIR_NAME = "Import";
    static String TRASH_DIR_NAME = "Trash";
    static String EXPORT_DIR_NAME = "Export";
    static String ID_DIR_NAME = "ID";
    static String DIR_BASE = "CloudCoins";
    private final int FIX_LOST = 0;
    private final int HEALTH_CHECK = 1;
    private final int FIX_FRACKED = 2;
    ProgressDialog progressDialog;
    boolean isStartupTaskCompleted = false;
    Handler mhandler;
    List<Settings> settingsList;
//    private View batteryProgressFill;
//    private View batteryProgressEmpty;
//
    private TextView tvTotalAmount, tvVersion;
    private LinearLayout llDeposit, llWithdraw;
    private List<SavedCoin> mCoinCount = new ArrayList<>();
    private RecyclerView rvDenomination;
    LinearLayout linearLayout;
    private String bankDirPath, limboDirPath, counterfeitPath, importPath, frackedDirPath, trashPath, mIDPath, exportPath;
    private IndicatorAdapter mAdapter;
    private ArrayList<Uri> coinsToDelete = new ArrayList<>();
    private ArrayList<EchoStatus> mList = new ArrayList<>(25);
    private RecyclerView rvIndicator;
    private LinearLayout llProgress;
    private int coinCount = 0, newCount = 0;
    private int fixTaskCode = 0;
    private boolean isFixing = false;
    private boolean isDetecting = true;
    private boolean isTicketing = false;
    private int totalApiResponseCount = 0, totalApiRequestCount = 0;
    private int echoCount = 0, echoPassCount = 0;
    private int mResponseCount = 0;

    private float echoWeight = 0;

    private RAIDAX raidax = RAIDAX.getInstance();
    //LinearLayout.LayoutParams layoutParams;
    @Nullable
    public static String getInternalStorageDirectoryPath(Context context) {
        String storageDirectoryPath;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (storageManager == null) {
                storageDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                ; //you can replace it with the Environment.getExternalStorageDirectory().getAbsolutePath()
            } else {
                storageDirectoryPath = storageManager.getPrimaryStorageVolume().getDirectory().getAbsolutePath();
            }
        } else {
            storageDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        return storageDirectoryPath;
    }

    @Override
    protected int defineLayoutResource() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initializeComponent(@NotNull View view) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        progressDialog = new ProgressDialog(getBaseActivity());
        settingsList = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .settingsDao().getAllStatus();

        tvTotalAmount = view.findViewById(R.id.home_fragment_tvPrice);
        tvVersion = view.findViewById(R.id.home_fragment_tvVerion);
        llDeposit = view.findViewById(R.id.home_fragment_llDeposit);
        llWithdraw = view.findViewById(R.id.home_fragment_llWithdraw);
        rvDenomination = view.findViewById(R.id.fragment_home_rvDenomination);
       // rvDenomination.setLayoutManager(new LinearLayoutManager(getActivity(),
       //         LinearLayoutManager.HORIZONTAL, true));
        rvIndicator = view.findViewById(R.id.fragment_home_rvIndicator);

        llProgress = view.findViewById(R.id.fragment_home_llProgress);

        int size = ScreeUtils.INSTANCE.getScreenWidth(getBaseActivity()) -
                getResources().getDimensionPixelSize(R.dimen._220sdp);
        int itemWidth = (size / 25);
        mAdapter = new IndicatorAdapter(getBaseActivity(), itemWidth);
        for (int i = 0; i < 25; i++) {
            mAdapter.addData(new EchoStatus(0, "Test"));
        }
        rvIndicator.setAdapter(mAdapter);
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
    }

    private void updateSettingDB(String desc, String status) {
        int count = 0;
        for (int i = 0; i < settingsList.size(); i++) {
            if (settingsList.get(i).getDescription().equals(desc))
                count++;
        }
        if (count > 0) {
            updateStatus(desc, status);
        } else {
            addStatus(desc, status);
        }
    }

    private void updateStatus(String desc, String val) {

        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .settingsDao().upDateSettings(val, desc);
    }

    private void addStatus(String desc, String status) {

        Settings settings = new Settings();
        settings.setDescription(desc);
        settings.setValue(status);
        settingsList.add(settings);

        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .settingsDao().insertSettings(settings);

    }

    @Override
    protected void initializeBehavior() {
        int size = ScreeUtils.INSTANCE.getScreenWidth(getBaseActivity()) -
                getResources().getDimensionPixelSize(R.dimen._220sdp);
            File path = new File(CouldcoinApplication.CloudcoinHome);
            if (path == null) {
                Log.e("TAGDirectory", "Failed to get External directory");
                return;
            }
            bankDirPath = CommonUtils.createDirectory(path, BANK_DIR_NAME, DIR_BASE);
            limboDirPath = CommonUtils.createDirectory(path, BANK_LIMBO_NAME, DIR_BASE);
            counterfeitPath = CommonUtils.createDirectory(path, COUNTERFEIT_DIR_NAME, DIR_BASE);
            importPath = CommonUtils.createDirectory(path, IMPORT_DIR_NAME, DIR_BASE);
            frackedDirPath = CommonUtils.createDirectory(path, FRACKED_DIR_NAME, DIR_BASE);
            trashPath = CommonUtils.createDirectory(path, TRASH_DIR_NAME, DIR_BASE);
            mIDPath = CommonUtils.createDirectory(path, ID_DIR_NAME, DIR_BASE);
            exportPath = CommonUtils.createDirectory(path, EXPORT_DIR_NAME, DIR_BASE);
            if (path == null) {
                Log.e("TAG", "Failed to get External directory");
                return;
            }

        if (settingsList.size() == 0) {
            updateSettingDB(MENU1, "true");
            updateSettingDB(MENU2, "true");
            updateSettingDB(MENU3, "true");
        }
        mCalculateCoins(0);

        setupListeners(false);
        PackageManager pm = getActivity().getPackageManager();
        String pkgName = getActivity().getPackageName();
        PackageInfo pkgInfo = null;
        try {
            pkgInfo = pm.getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void takePermission() {

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                Uri uri = Uri.fromParts("package", "com.cloudcoin2.wallet", null);
                intent.setData(uri);
                startActivityForResult(intent, 101);
            }
            catch(Exception exception){
                exception.printStackTrace();
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length>0){
            if(requestCode==101){
                boolean readtext=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                if(!readtext){
                    takePermission();
                }
                else{
                    File path = new File(getInternalStorageDirectoryPath(getBaseActivity()));
                    //String path=getBaseActivity().getExternalFilesDir(null).getAbsolutePath();
                    if (path == null) {
                        Log.e("TAGDirectory", "Failed to get External directory");
                        return;
                    }
                    bankDirPath = CommonUtils.createDirectory(path, BANK_DIR_NAME, DIR_BASE);
                    limboDirPath = CommonUtils.createDirectory(path, BANK_LIMBO_NAME, DIR_BASE);
                    counterfeitPath = CommonUtils.createDirectory(path, COUNTERFEIT_DIR_NAME, DIR_BASE);
                    importPath = CommonUtils.createDirectory(path, IMPORT_DIR_NAME, DIR_BASE);
                    frackedDirPath = CommonUtils.createDirectory(path, FRACKED_DIR_NAME, DIR_BASE);
                    trashPath = CommonUtils.createDirectory(path, TRASH_DIR_NAME, DIR_BASE);
                    mIDPath = CommonUtils.createDirectory(path, ID_DIR_NAME, DIR_BASE);
                    exportPath = CommonUtils.createDirectory(path, EXPORT_DIR_NAME, DIR_BASE);
                    if (path == null) {
                        Log.e("TAG", "Failed to get External directory");
                        return;
                    }
                }
            }
        }
    }

    private void mCalculateCoins(int flag) {
        if (flag == 0) {
            mCoinCount = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                    .coinDao().//getAll();
                    getAllBank("Bank", "Fracked");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte value = -5; // Your byte value
                    String bits = "";
                    // Print bits in the byte
                    for (int i = 7; i >= 0; i--) {
                        int bit = (value >> i) & 1;
                        bits += bit;
                        System.out.print(bit);
                    }
                    Log.d(RAIDAX.TAG, "Bits:" + bits + ". Hex: " + Integer.toHexString(value));

                    System.out.println();
                    try {
                        List<CloudCoin> ccs =  CloudCoinFileWriter.loadCoinsFromFolder(bankDirPath);
                        List<CloudCoin> ccsf =  CloudCoinFileWriter.loadCoinsFromFolder(frackedDirPath);

                        Log.d(RAIDAX.TAG, "Coins Loaded:" + ccs.size());
                        int i =0;
                        double total = 0;
                        for (CloudCoin cc:
                             ccs) {
                            total += Denominations.getFraction(cc.getDenomination());
                            //Log.d(RAIDAX.TAG, "Coin  " + i++ + " : " + cc.getDenomination() + ". Serial :" + cc.getSerialAsInt() + ", Fraction: "+ total);
                        }
                        for (CloudCoin cc:
                                ccsf) {
                            total += Denominations.getFraction(cc.getDenomination());
                            //Log.d(RAIDAX.TAG, "Coin  " + i++ + " : " + cc.getDenomination() + ". Serial :" + cc.getSerialAsInt() + ", Fraction: "+ total);
                        }

                        DecimalFormat decimalFormat = new DecimalFormat("#####");
                        String stringValue = decimalFormat.format(total);

                        int bitcoinBalance = (int) total;
                        long satoshiBalance = (long) ((total - bitcoinBalance) * 1e8);

                        String bitcoinBalanceText = String.format("%d", bitcoinBalance);
                        String satoshiBalanceText = String.format("%08d", satoshiBalance);

                        Log.d(RAIDAX.TAG,"Total: "+ stringValue);
                        int bankCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Bank"),".bin");
                        int frackedCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Fracked"),".bin");
                        tvTotalAmount.setText(satoshiBalanceText);
                    }
                    catch (Exception e) {
                        Log.e("Error", e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } else {
            int oldcount = mCoinCount.size();
            mCoinCount.clear();
            mCoinCount = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                    .coinDao().//getAll();
                    getAllBank("Bank", "Fracked");
            coinCount = mCoinCount.size();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int bankCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Bank"),".bin");
                        int frackedCoins = CommonUtils.countFiles(CommonUtils.getFolderPath(getActivity(),"Fracked"),".bin");
                        tvTotalAmount.setText(String.valueOf(bankCoins + frackedCoins));
                    }
                    catch (Exception e) {
                        Log.e("Error", e.getMessage());
                        e.printStackTrace();
                    }

                    //tvTotalAmount.setText(String.valueOf(mCoinCount.size()));
                }
            });
            if (coinCount != oldcount) {
                Transactions task = new Transactions();
                task.setDate(String.valueOf(System.currentTimeMillis()));
                task.setAmount(String.valueOf(oldcount - coinCount));
                task.setMemo("Coin found counterfeit in health check");
                task.setType("1");

                DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .transactionDao()
                        .insertAll(task);
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // refreshRaida();
    }

    private boolean checkHealthCheckSettings(int checkType) {
        boolean istrue = false;
        List<Settings> SettingsList = DatabaseClient.getInstance(getActivity()).getAppDatabase().
                settingsDao().getAllStatus();
        if (SettingsList.size() > 0) {
            if (checkType == 1) {
                for (int i = 0; i < SettingsList.size(); i++) {
                    if (SettingsList.get(i).getDescription().equals("Health Check"))
                        if (SettingsList.get(i).getValue().equals("true"))
                            istrue = true;
                        else
                            istrue = false;
                }
                return istrue;
            } else if (checkType == 2) {
                for (int i = 0; i < SettingsList.size(); i++) {
                    if (SettingsList.get(i).getDescription().equals("Fix Frack"))
                        if (SettingsList.get(i).getValue().equals("true"))
                            istrue = true;
                        else
                            istrue = false;
                }
                return istrue;
            } else if (checkType == 0) {
                for (int i = 0; i < SettingsList.size(); i++) {
                    if (SettingsList.get(i).getDescription().equals("Fix Lost"))
                        if (SettingsList.get(i).getValue().equals("true"))
                            istrue = true;
                        else
                            istrue = false;
                }
                return istrue;
            }
        } else {
            return istrue;
        }
        return istrue;
    }

    private void showProgressDialogWithTitle(String title, String substring) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setTitle(title);
                progressDialog.setMessage(substring);
                progressDialog.show();
            }
        });

    }

    // Method to hide/ dismiss Progress bar
    private void hideProgressDialogWithTitle() {
        //progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }

    public void performStartupTasks(int taskCode) {
        mResponseCount = 0;
        Log.d("Task", "Response count:" + mResponseCount);
        List<DetectionCoinsModel> savedCoins = new ArrayList<>();
        if (taskCode == 1) {
            showProgressDialogWithTitle("Startup tasks in progress", "Verifying and fixing Coins");
            fixTaskCode = 1;
            Log.d("Task", "Detecting Coins");
            savedCoins = prepareDetectionFileList();
            // coinsToDelete.addAll(savedCoins);
        } else if (taskCode == 0) {
            // showProgressDialogWithTitle("Startup tasks in Progress","Please wait for Fix Lost to Complete");
            Log.d("Task", "Fixing Lost Coins");
            savedCoins = prepareLostFileList();
            //coinsToDelete.addAll(savedCoins);
        } else {
            showProgressDialogWithTitle("Startup tasks in progress", "Verifying and fixing Coins");

            fixTaskCode = 2;
            Log.d("Task", "Fixing fracked Coins");
            savedCoins = prepareFrackedFileList();
            //coinsToDelete.addAll(savedCoins);

        }
        if (savedCoins.size() > 0) {
            List<CloudCoin> coins = new ArrayList<>();
            RAIDA raida = RAIDA.getInstance();
            //raida.setDebug(true);
            raida.setCallbacks(this);
            for (int i = 0; i < savedCoins.size(); i++) {
                File file = new File(savedCoins.get(i).getUri().getPath());
                byte[] bytes = KotlinUtils.INSTANCE.readBinaryFile(file.getPath());
                try {
                    assert bytes != null;
                    CloudCoin coin = raida.binaryToCoin(bytes);
                    coin.setPath(savedCoins.get(i).getUri().getPath());
                    coin.setCoinType(savedCoins.get(i).getStatus());
                    coins.add(coin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                switch (taskCode) {
                    case 0:
                        Log.d("Task", "Starting - Fixing lost coin task");
                        break;
                    case 1:
                        Log.d("Task", "Starting - health check task");
                        raida.detect(coins);
                        break;
                    case 2:
                        Log.d("Task", "Starting - fix task");
                        fixRaida(coins);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                hideProgressDialogWithTitle();

            }

        }
        else
        {
            hideProgressDialogWithTitle();
        }
      //
    }

    public void deleteCoin(String path) throws Exception {

        String trashPath = path.replace("Bank", "Trash");
        trashPath = trashPath.replace("Fracked", "Trash");

        moveCoin(path, trashPath);
    }

    private void moveCoin(String fromCoin, String toCoin) {


        File file = new File(fromCoin);


        if(!file.exists() || !file.isFile())         // Check if it's a regular file
        {
            return;
        }
        Log.d("Startup","Moving:"+fromCoin+" to "+toCoin);


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
            Log.e("file error", fnfe1.getMessage());
        } catch (Exception e) {
            Log.e("file error", e.getMessage());
        }

    }


    private void updateDB(byte[] serial, String pStatus) {
        int pSerial = new BigInteger(serial).intValue();
        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .coinDao()
                .upDateCoinStatus(pSerial, pStatus);

    }

    private void saveFile(CloudCoin coin, String DirPath) {

        try {
            byte[] binary = RAIDA.getInstance().coinToBinary(coin);
            File file = new File(DirPath, coin.getFileName());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(binary);
                // fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<DetectionCoinsModel> prepareDetectionFileList() {

        ArrayList<DetectionCoinsModel> filePaths = new ArrayList<>();
        int index = 0;
        int authCoinCount = CommonUtils.getCoinCount(bankDirPath);
        int frackedCoinCount = CommonUtils.getCoinCount(frackedDirPath);
        int lostCoinCount = CommonUtils.getCoinCount(limboDirPath);

        if (authCoinCount > 0) {
            File file = new File(bankDirPath);
            File[] fList = file.listFiles();

            if (fList != null) {
                for (int i = 0; i < fList.length; i++) {
                    DetectionCoinsModel dcModel = new DetectionCoinsModel();
                    dcModel.setUri(Uri.fromFile(fList[i]));
                    dcModel.setStatus(0);
                    filePaths.add(dcModel);

                }
            }
        }

        if (frackedCoinCount > 0) {
            File file = new File(frackedDirPath);
            File[] fList = file.listFiles();

            if (fList != null) {
                for (int i = 0; i < fList.length; i++) {
                    DetectionCoinsModel dcModel = new DetectionCoinsModel();
                    dcModel.setUri(Uri.fromFile(fList[i]));
                    dcModel.setStatus(0);
                    filePaths.add(dcModel);
                }
            }
        }

        if (lostCoinCount > 0) {
            File file = new File(limboDirPath);
            File[] fList = file.listFiles();

            if (fList != null) {
                for (File value : fList) {
                    DetectionCoinsModel dcModel = new DetectionCoinsModel();
                    dcModel.setUri(Uri.fromFile(value));
                    dcModel.setStatus(0);
                    filePaths.add(dcModel);
                }
            }
        }

        Log.d("filepaths", filePaths.toString());
        return filePaths;
    }

    private List<DetectionCoinsModel> prepareFrackedFileList() {
        int index = 0;
        ArrayList<DetectionCoinsModel> filePaths = new ArrayList<>();
        int frackedcoinCount = CommonUtils.getCoinCount(frackedDirPath);

        if (frackedcoinCount > 0) {
            File file = new File(frackedDirPath);
            coinsToDelete.add(Uri.fromFile(file));
            File[] fList = file.listFiles();

            if (fList != null) {
                for (File value : fList) {
                    DetectionCoinsModel dcModel = new DetectionCoinsModel();
                    dcModel.setUri(Uri.fromFile(value));
                    dcModel.setStatus(0);
                    filePaths.add(dcModel);
                }
            }
        }

        Log.d("filepaths", filePaths.toString());
        return filePaths;
    }

    private List<DetectionCoinsModel> prepareLostFileList() {
        int index = 0;
        ArrayList<DetectionCoinsModel> filePaths = new ArrayList<>();
        int frackedcoinCount = CommonUtils.getCoinCount(limboDirPath);

        if (frackedcoinCount > 0) {
            File file = new File(limboDirPath);
            File[] fList = file.listFiles();

            if (fList != null) {
                for (File value : fList) {
                    DetectionCoinsModel dcModel = new DetectionCoinsModel();
                    dcModel.setUri(Uri.fromFile(value));
                    dcModel.setStatus(0);
                    filePaths.add(dcModel);
                }
            }
        }
        Log.d("filepaths", filePaths.toString());
        return filePaths;

    }

    private void fixRaida(List<CloudCoin> coins) {
        try {

            mResponseCount = 0;
            Log.d("FixRaida", "Response count:" + mResponseCount);
            isFixing = false;
            isDetecting = true;
            isTicketing = false;
            RAIDA raida = RAIDA.getInstance();
           // raida.setDebug(true);
            raida.detect(coins);
            Log.d("FixRaida", "Task Code:" + String.valueOf(fixTaskCode));
            Log.d("FixRaida", "Startup fix fracked initial detection call completed");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void FixFrackCalculation() {
        List<CloudCoin> coins = RAIDA.getInstance().getCloudCoins();
        if (RAIDA.getInstance().getAuthenticCoins().size() > 0) {
            Log.d("Fix Fracked", "Adding " + RAIDA.getInstance().getAuthenticCoins().size() +
                    " authentic coins back to the list");
            coins.addAll(RAIDA.getInstance().getAuthenticCoins());
        }

        Log.d("TEST", "FIX result for " + coins.size() + " coins");
        int totalCoins = coins.size();
        for (int j = 0; j < coinsToDelete.size(); j++) {
            try {
                deleteCoin(coinsToDelete.get(j).getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        coinsToDelete.clear();
        //hideProgressDialogWithTitle();
        for (int i = 0; i < totalCoins; i++) {
            Log.d("TEST", "COIN:" + RAIDA.bytesToHex(coins.get(i).getSerial()) + "");
            if (coins.get(i).getPassCount() == 25) {
                Log.d("TEST", "Coin " + coins.get(i).getFileName() + " is fully authentic");
                saveFile(coins.get(i), bankDirPath);
                updateDB(coins.get(i).getSerial(), "Bank");
            } else if (coins.get(i).getPassCount() >= 16) {
                Log.d("TEST", "Coin " + coins.get(i).getFileName() + " is fracked2 at " + coins.get(i)
                        .getFailCount() + " places, no response from " + coins.get(i).getNoResponseCount() + " raidas");
                saveFile(coins.get(i), frackedDirPath);
                updateDB(coins.get(i).getSerial(), "Fracked");
            } else if (coins.get(i).getFailCount() > 9) {
                Log.d("TEST", "Coin " + coins.get(i).getFileName() + " is counterfeit because "
                        + coins.get(i).getFailCount() + " raidas returned allfail");
                saveFile(coins.get(i), counterfeitPath);
                updateDB(coins.get(i).getSerial(), "Fracked");
            } else {
                Log.d("TEST", "Coin " + coins.get(i).getFileName() + " is lost/in limbo and " +
                        "needs to be found because - pass: " + coins.get(i).getPassCount() + ", fail: " +
                        coins.get(i).getFailCount() + ", no response: " + coins.get(i).getNoResponseCount());
                saveFile(coins.get(i), limboDirPath);
                updateDB(coins.get(i).getSerial(), "Fracked");
            }

        }

        fixTaskCode = 0;
        hideProgressDialogWithTitle();

    }

    private void calculateDetection(List<CloudCoin> coins) {
        int totalCoins = coins.size();
        int authentic = 0;
        int counterfeit = 0;
        int fracked = 0;
        int lost = 0;

        for (int i = 0; i < totalCoins; i++) {
            Log.d("detectcoin", coins.get(i).toString());
            // Log.d("TEST", "COIN:"+ RAIDA.bytesToHex(coins.get(i).serial)+"")
            if (coins.get(i).getPassCount() == 25) {
                Log.d("Detection Response", "Coin " + coins.get(i).getFileName() +
                        " is fully authentic");
                if (coins.get(i).getCoinType() != 0) {
                    try {
                        deleteCoin(coins.get(i).getPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    saveFile(coins.get(i), bankDirPath);
                    updateDB(coins.get(i).getSerial(), "Bank");

                }
                authentic++;
            } else if (coins.get(i).getPassCount() >= 16) {
                Log.d("Detection Response", "Coin " + coins.get(i).getFileName() +
                        " is fracked at " + coins.get(i)
                        .getFailCount() + " places, no response from " + coins.get(i).
                        getNoResponseCount() + " raidas");
                if (coins.get(i).getCoinType() != 1) {
                    try {
                        deleteCoin(coins.get(i).getPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    saveFile(coins.get(i), frackedDirPath);
                    updateDB(coins.get(i).getSerial(), "Fracked");

                }
                authentic++;
                fracked++;
            } else if (coins.get(i).getFailCount() > 9) {
                Log.d("Detection Response", "Coin " + coins.get(i).getFileName() +
                        " is counterfeit because "
                        + coins.get(i)
                        .getFailCount() + " raidas returned allfail");

                if (coins.get(i).getCoinType() != 2) {
                    try {
                        deleteCoin(coins.get(i).getPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    saveFile(coins.get(i), counterfeitPath);
                    updateDB(coins.get(i).getSerial(), "Counterfeit");

                }
                counterfeit++;
            } else {
                Log.d("Detection Response", "Coin " + coins.get(i).getFileName() +
                        " is lost/in limbo and " + "needs to be found because - pass: "
                        + coins.get(i).getPassCount() + ", fail: " + coins.get(i).getFailCount() +
                        ", no response: " + coins.get(i).getNoResponseCount());
                lost++;
            }


        }
        Log.d("Detection Result", "Total Coins: " + totalCoins + ", Authentic: " + authentic +
                ", Fracked: " + fracked + ", Counterfeit: " +
                counterfeit + ", Lost: " + lost);
        mCalculateCoins(1);
        fixTaskCode = 0;
        hideProgressDialogWithTitle();
        boolean isFixFrackedEnabled = checkHealthCheckSettings(FIX_FRACKED);
        if (isFixFrackedEnabled) {

            Log.d("startup", "Starting Fix Frack after detection");
            performStartupTasks(2);
        }

    }

    private void handleHealthCheckResponse() {
        Log.d("Detect - Health check", "Task Code:" + String.valueOf(fixTaskCode));
        mResponseCount++;
        Log.d("Detect - Health check", "Response count:" + mResponseCount);

        if (mResponseCount == 25) {
            mResponseCount = 0;
            Log.d("Detect - Health check", "Response count:" + mResponseCount);
            List<CloudCoin> coinList = RAIDA.getInstance().getCloudCoins();
            calculateDetection(coinList);
        }
    }

    private void handleFixFrackProcess(String hex, int commandCode, int raidaID) {
        RAIDA raida = RAIDA.getInstance();
        Log.d("RAIDA" + raidaID, "Current: Response count " + mResponseCount + " Detecting:" + isDetecting + " Ticketing:" + isTicketing + " Fixing:" + isFixing);

        boolean performFinalCalculation = true;

        if (mResponseCount < 25) {
            if (isDetecting && commandCode == 1) {
                mResponseCount++;
                Log.d("Fix Fracked", "Detecting Response count:" + mResponseCount + " RAIDA response Count:"+raida.raidaResponses.size());
            }

            if (isTicketing && commandCode == 11) {
                mResponseCount++;
                Log.d("Fix Fracked", "Ticketing Response count:" + mResponseCount + " RAIDA response Count:"+raida.raidaResponses.size());
            }
            if (isFixing && commandCode == 3) {
                mResponseCount++;
                int no = RAIDA.getInstance().getNumFixRaidas();
                Log.d("Fix Fracked", "Fixable Raidas:" + no);

                Log.d("Fix Fracked", "Fixing Response count:" + mResponseCount);
            }
            // hideProgressDialogWithTitle();
        }


        if (mResponseCount == 25) {
            Log.d("Fix Fracked", "Response count reached " + mResponseCount + " moving to next step");
            if (isDetecting) {
                mResponseCount = 0;
                Log.d("Fix Fracked", "Detecting Response count:" + mResponseCount);
                isDetecting = false;
                isTicketing = true;
                isFixing = false;
                boolean anyFracked = false;
                // check if there are any fracked coins
                List<CloudCoin> coinList = RAIDA.getInstance().getCloudCoins();
                for (int i = 0; i < coinList.size(); i++) {
                    if(coinList.get(i).getPassCount()>=16 && coinList.get(i).getFailCount()>0 )
                    {
                        anyFracked = true;
                        break;
                    }
                }
                if(anyFracked)
                {
                    try {

                        //raida.setDebug(true);
                        raida.setCallbacks(this);
                        Log.d("Fix Fracked", "Getting Tickets");
                        raida.getTicket();
                        //Log.d("hashcode1", String.valueOf(RAIDA.getInstance().hashCode()));
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
                else
                {
                    Log.d("Fix Fracked","No Fracked coins to fix");
                    hideProgressDialogWithTitle();
                }

            } else if (isTicketing) {
                isDetecting = false;
                isTicketing = false;
                mResponseCount = 0;
                Log.d("Fix Fracked", "Ticketing Response count:" + mResponseCount + " RAIDA response Count:"+raida.raidaResponses.size());
                isFixing = true;
                try {

                    Log.d("Fix Fracked", "Fixing Fracked Coins with obtained tickets");
                  //  raida.setDebug(true);
                    raida.setCallbacks(this);
                    if (raida.raidaResponses != null && raida.raidaResponses.size() == 25) {
                        hideProgressDialogWithTitle();
                        showProgressDialogWithTitle("Startup tasks in progress", "Fixing fractured coins");
                        raida.fix();
                        if(raida.getNumFixableCoins() ==0) // no fixable coins
                        {
                            hideProgressDialogWithTitle();
                        }
                    } else {
                        Log.d("Fix Fracked", "Exiting the fix fracked process as we could not obtain 25 responses to Get Ticket service. Responses obtained:" + raida.raidaResponses.size());
                        performFinalCalculation = false;
                        hideProgressDialogWithTitle();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Fix Fracked", "Exiting fix fracked process, since fixing is not possible at the moment");
                    performFinalCalculation = false;
                    hideProgressDialogWithTitle();
                }
            } else if (isFixing) {
                mResponseCount = 0;
                if (performFinalCalculation) {
                    Log.d("Fix Fracked", "Performing final calculation to update wallet after fixing fracked");
                    FixFrackCalculation();

                    Log.d("Fix Fracked", "Fixing Response count:" + mResponseCount);
                }
            }

        } else if (isFixing && mResponseCount == RAIDA.getInstance().getNumFixRaidas() && RAIDA.getInstance().getNumFixRaidas() > 0) {
            mResponseCount = 0;
            if (performFinalCalculation) {
                Log.d("Fix Fracked", "Performing final calculation to update wallet after fixing fracked");
                FixFrackCalculation();
            }
            Log.d("Fix Fracked", "Fixing Response count:" + mResponseCount);
        }

        Log.d("RAIDA "+raidaID, "RESPONSE:"+hex + " for CMD:" + commandCode);
        // hideProgressDialogWithTitle();
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

            echoWeight = result.getPassCount()*100/ RAIDAX.NUM_SERVERS;

// Set the weight for the view
    //setBatteryPercentage(0.6f);

            //layoutParams.weight = echoWeight;

// Update the view's layout parameters
//            batteryProgressFill.setLayoutParams(layoutParams);
//            batteryProgressEmpty.setLayoutParams(layoutParams2);

            rvIndicator.post(new Runnable() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });

            setupListeners(true);

        }
        catch (Exception e) {

        }

    }

    private void handleEchoCallback(String hex, int raidaID) {
        echoCount++;
        Log.d("Echo", "Echo Response count: " + echoCount);
        EchoStatus status = new EchoStatus(raidaID, "failure");
        if (hex != "") {
            char[] ch = new char[hex.length()];
            for (int i = 0; i < hex.length(); i++) {
                ch[i] = hex.charAt(i);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(ch[4]).append(ch[5]);
            String stats = sb.toString();

            if (stats.equals("FA")) {
                status = new EchoStatus(raidaID, "success");
                echoPassCount++;
            }
        }
        mAdapter.replaceData(status, raidaID);
        rvIndicator.post(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });

        Log.d("Echo", "Echo Response count: " + echoCount);
        Log.d("Echo", "Pass count: " + String.valueOf(echoPassCount));
        if (echoCount == 25) {
            setupListeners(true);
            echoCount = 0;
            if (Constants.isStartupTask) {
                if (echoPassCount > 16) {
                    echoPassCount = 0;
                    Constants.isStartupTask = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // showProgressDialogWithTitle("Startup tasks in Progress","Please wait for Startup Tasks to Complete");
                            //ToDo-Echo Count Check must be greater than 16
                            boolean isHealthCheckEnabled = checkHealthCheckSettings(HEALTH_CHECK);
                            boolean isFixFrackedEnabled = checkHealthCheckSettings(FIX_FRACKED);
                            boolean isFixLostEnabled = checkHealthCheckSettings(FIX_LOST);
                            // if fix fracked is enabled, then no need to detect separately because
                            // during fix fracked, detection will be done in step 1 anyway
                            if (isHealthCheckEnabled) {
                                Log.d("startup", "Starting Detection Task");
                                performStartupTasks(1);

                            } else if (isFixFrackedEnabled) {
                                Log.d("startup", "Starting Fix Frack");
                                performStartupTasks(2);

                            } else if (isFixLostEnabled) {
                                Log.d("startup", "Starting Fix Lost");
                                performStartupTasks(0);

                            }
                            //  hideProgressDialogWithTitle();
                        }
                    }).start();
                }
            }
        }
    }


    public void setupListeners(boolean isEchoCompleted) {
        if (isEchoCompleted) {
            llDeposit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Navigation.findNavController(view).navigate(R.id.depositFragment);
                }
            });
            llWithdraw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Navigation.findNavController(view).navigate(R.id.withdrawFragment);
                }
            });
        } else {
            llDeposit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getBaseActivity(), "Contacting RAIDA servers. Please wait.", Toast.LENGTH_SHORT).show();
                }
            });
            llWithdraw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getBaseActivity(), "Contacting RAIDA servers. Please wait.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void ReportBack(byte[] lMsg, String hex, int commandCode, int raidaID, int passcount) {
        if (commandCode == 4) {
            Log.d("RAIDA " + raidaID, "Success callback for command code " + commandCode);
            handleEchoCallback(hex, raidaID);

        } else if (commandCode == 1 ||  commandCode == 11 || commandCode == 3) {
            Log.d("commandcode", String.valueOf(commandCode));
            if (fixTaskCode == 1) {
                Log.d("RAIDA " + raidaID, "Success callback for command code " + commandCode + " and task code " + fixTaskCode);
                handleHealthCheckResponse();
            }
            if (fixTaskCode == 2 ||  commandCode == 11 || commandCode == 3) {
                Log.d("RAIDA " + raidaID, "Success callback for command code " + commandCode + " and task code " + fixTaskCode);
                handleFixFrackProcess(hex, commandCode, raidaID);
            }
        }
    }

    @Override
    public void ReportBackError(Exception e, byte[] data, int commandCode, int raidaID) {
        Log.d("RAIDA " + raidaID, "Error callback for command code " + commandCode);
        if (commandCode == 4) {

            Log.d("RAIDA " + raidaID, "Error callback for command code " + commandCode);
            handleEchoCallback("", raidaID);
        } else if (commandCode == 1 ||  commandCode == 11 || commandCode == 3) {

            if (fixTaskCode == 1) {
                Log.d("RAIDA " + raidaID, "Error callback for command code " + commandCode + " and task code " + fixTaskCode);
                handleHealthCheckResponse();
            }
            if (fixTaskCode == 2) {
                Log.d("RAIDA " + raidaID, "Error callback for command code " + commandCode + " and task code " + fixTaskCode);
                handleFixFrackProcess("Error", commandCode, raidaID);
            }
        }

    }
}
