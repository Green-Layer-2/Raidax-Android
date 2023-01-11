package com.cloudcoin2.wallet.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.cloudcoin2.wallet.R;
import com.cloudcoin2.wallet.Utils.CommonUtils;
import com.cloudcoin2.wallet.Utils.Constants;
import com.cloudcoin2.wallet.Utils.CouldcoinApplication;
import com.cloudcoin2.wallet.Utils.SharedPref;
import com.cloudcoin2.wallet.base.BaseFragment2;
import com.cloudcoin2.wallet.db.DatabaseClient;
import com.cloudcoin2.wallet.db.Settings;
import com.cloudcoin2.wallet.db.SettingsDao;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SettingsFragment extends BaseFragment2 implements View.OnClickListener {

    SwitchCompat button1, button2, button3;
    ImageView imageView;
    ImageView imageExport;
    ImageView imageClear;
    private static final String MENU1 = "Fix Lost";
    private static final String MENU2 = "Health Check";
    private static final String MENU3 = "Fix Frack";
    List<Settings> settingsList = DatabaseClient.getInstance(getActivity()).getAppDatabase()
            .settingsDao().getAllStatus();
    List<String> filesListInDir = new ArrayList<String>();
    static String DIR_BASE = "CloudCoins";
    AlertDialog.Builder builder;
    @Override
    protected int defineLayoutResource() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void initializeComponent(@NonNull View view) {
        button1 = view.findViewById(R.id.toggle1);
        button2 = view.findViewById(R.id.toggle2);
        button3 = view.findViewById(R.id.toggle3);
        imageView = view.findViewById(R.id.imageView3);
        imageExport = view.findViewById(R.id.imageViewExport);
        imageClear = view.findViewById(R.id.imageViewDelete);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        imageView.setOnClickListener(this);
        imageExport.setOnClickListener(this);
        imageClear.setOnClickListener(this);

    }

    @Override
    protected void initializeBehavior() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String toggleStatus1, toggleStatus2, toggleStatus3;
        for (int i = 0; i < settingsList.size(); i++) {
            if (settingsList.get(i).getDescription().equals(MENU1)) {
                toggleStatus1 = settingsList.get(i).getValue();
                if (toggleStatus1.equals("true"))
                    button1.setChecked(true);
                else
                    button1.setChecked(false);
            } else if (settingsList.get(i).getDescription().equals(MENU2)) {
                toggleStatus2 = settingsList.get(i).getValue();
                if (toggleStatus2.equals("true"))
                    button2.setChecked(true);
                else
                    button2.setChecked(false);
            } else if (settingsList.get(i).getDescription().equals(MENU3)) {
                toggleStatus3 = settingsList.get(i).getValue();
                if (toggleStatus3.equals("true"))
                    button3.setChecked(true);
                else
                    button3.setChecked(false);
            }
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.toggle1:
                boolean state = button1.isChecked();
                if (state) {
                    updateDB(MENU1, "true");
                } else
                    updateDB(MENU1, "false");
                break;
            case R.id.toggle2:
                boolean state2 = button2.isChecked();
                if (state2) {
                    updateDB(MENU2, "true");
                } else
                    updateDB(MENU2, "false");
                break;
            case R.id.toggle3:
                boolean state3 = button3.isChecked();
                if (state3) {
                    updateDB(MENU3, "true");
                } else
                    updateDB(MENU3, "false");
                break;
            case R.id.imageViewExport:
                zipExport();
                break;
            case R.id.imageViewDelete:
                cleanupExport();
                break;
            case R.id.imageView3:
                openShareDialog();
                break;
        }

    }

    private void openShareDialog(String filePath) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File infile=new File(filePath);
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

    private void zipExport() {
        String basePath = CouldcoinApplication.CloudcoinHome + "/" + DIR_BASE + "/Export" ;
        File expDir = new File(basePath);
        String exportFile =CouldcoinApplication.CloudcoinHome + "/" + DIR_BASE +"/exportcoins" + CommonUtils.getCurrentDateTime() + ".zip";
        zipDirectory(expDir, exportFile);
        Toast.makeText(getBaseActivity(), "Export folder backed up",Toast.LENGTH_LONG).show();
        openShareDialog(exportFile);
//        String sourceFile = basePath;
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + DIR_BASE +"/dirCompressed.zip");
//            ZipOutputStream zipOut = new ZipOutputStream(fos);
//
//            File fileToZip = new File(sourceFile);
//            zipFile(fileToZip, fileToZip.getName(), zipOut);
//            zipOut.close();
//            fos.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private  void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    private void cleanupExport() {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Cleanup export")
                .setMessage("Cleanup export folder?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String basePath = CouldcoinApplication.CloudcoinHome + "/" + DIR_BASE + "/" + "Export";
                        File expDir = new File(basePath);
                        for (File child : expDir.listFiles())
                            child.delete();

                        Toast.makeText(getBaseActivity(), "Export folder is now empty.",Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        //Creating dialog box
        AlertDialog dialog  = builder.create();
        dialog.show();
    }

    private void openShareDialog() {
        String logPath = Constants.LOG_PATH;
        File infile = new File(logPath);
        // Toast.makeText(getBaseActivity(),"File path is "+ logPath,Toast.LENGTH_LONG).show();
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


    private void updateDB(String desc, String status) {
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

    private void addStatus(String desc, String status) {
        Settings settings = new Settings();
        settings.setDescription(desc);
        settings.setValue(status);
        settingsList.add(settings);

        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .settingsDao().insertSettings(settings);

    }

    private void updateStatus(String desc, String val) {

        DatabaseClient.getInstance(getActivity()).getAppDatabase()
                .settingsDao().upDateSettings(val, desc);
    }

    /**
     * This method zips the directory
     * @param dir
     * @param zipDirName
     */
    private void zipDirectory(File dir, String zipDirName) {
        try {
            populateFilesList(dir);
            //now zip files one by one
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipDirName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for(String filePath : filesListInDir){
                System.out.println("Zipping "+filePath);
                //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
                zos.putNextEntry(ze);
                //read the file and write to ZipOutputStream
                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method populates all the files in a directory to a List
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }

    /**
     * This method compresses the single file to zip format
     * @param file
     * @param zipFileName
     */
    private static void zipSingleFile(File file, String zipFileName) {
        try {
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            //add a new Zip Entry to the ZipOutputStream
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);
            //read the file and write to ZipOutputStream
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            //Close the zip entry to write to zip file
            zos.closeEntry();
            //Close resources
            zos.close();
            fis.close();
            fos.close();
            System.out.println(file.getCanonicalPath()+" is zipped to "+zipFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
