package com.cloudcoin2.wallet.BottomSheet;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.cloudcoin2.wallet.R;

public class PownSummaryBottomSheet extends BottomSheetBaseFragment implements BottomSheetInterface {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARAM3 = "param3";
    private static final String ARG_PARAM4 = "param4";
    private static final String ARG_PARAM5 = "param5";
    private static final String TAG = "PownSummarysheet";
    private String mParam2, mParam3, mParam1, mParam4, mParam5;
    TextView totalCoins, authCoins, lostCoins, cfeitCoins, duplicate;
    TextView button_ok;
    BottomSheetInterface bottominterface;

    public PownSummaryBottomSheet() {
    }

    @Override
    public int getLayout() {
        return R.layout.dialog_pownsummary;
    }

    public static PownSummaryBottomSheet newInstance(FragmentManager fragmentManager, int totalcount, int authcount, int counterfeit, int Lostcount, BottomSheetInterface bottomSheetInterface, int sDuplicateCount) {
        PownSummaryBottomSheet fragment = new PownSummaryBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, String.valueOf(totalcount));
        args.putString(ARG_PARAM2, String.valueOf(authcount));
        args.putString(ARG_PARAM3, String.valueOf(counterfeit));
        args.putString(ARG_PARAM4, String.valueOf(Lostcount));
        args.putString(ARG_PARAM5, String.valueOf(sDuplicateCount));
        fragment.bottominterface = bottomSheetInterface;
        fragment.setArguments(args);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            mParam3 = getArguments().getString(ARG_PARAM3);
            mParam4 = getArguments().getString(ARG_PARAM4);
            mParam5 = getArguments().getString(ARG_PARAM5);
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean isMore = false;
        totalCoins = view.findViewById(R.id.totalCoinSize);
        authCoins = view.findViewById(R.id.authentic);
        lostCoins = view.findViewById(R.id.LostCoinsize);
        cfeitCoins = view.findViewById(R.id.Counterfeit);
        duplicate = view.findViewById(R.id.Duplicate);
        button_ok = view.findViewById(R.id.okbutton);
        button_ok.setText("OK");
        int totalc = Integer.parseInt(mParam1);
        isMore = checkCount(totalc);
        if (isMore)
            totalCoins.setText(mParam1.concat(" Coins Deposited"));
        else
            totalCoins.setText(mParam1.concat(" Coin Deposited"));

        int passed = Integer.parseInt(mParam2);
        isMore = checkCount(passed);
        if (isMore)
            authCoins.setText("Passed - ".concat(mParam2).concat(" Coins"));
        else
            authCoins.setText("Passed - ".concat(mParam2).concat(" Coin"));

        int counter = Integer.parseInt(mParam3);
        isMore = checkCount(counter);
        if (isMore)
            cfeitCoins.setText("Counterfeit - ".concat(mParam3).concat(" Coins"));
        else
            cfeitCoins.setText("Counterfeit - ".concat(mParam3).concat(" Coin"));

        int lost = Integer.parseInt(mParam4);
        isMore = checkCount(lost);
        if (isMore)
            lostCoins.setText("Lost - ".concat(mParam4).concat(" Coins"));
        else
            lostCoins.setText("Lost - ".concat(mParam4).concat(" Coin"));

        int duplicateCount = Integer.parseInt(mParam5);
        isMore = checkCount(duplicateCount);
        if (isMore)
            duplicate.setText("Skipped Coins - ".concat(mParam5).concat(" Coins"));
        else
            duplicate.setText("Skipped Coins - ".concat(mParam5).concat(" Coin"));

        button_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottominterface.Onclick();
                dismiss();
            }
        });

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        //Code here
    }

    private boolean checkCount(int passed) {
        if (passed != 1)
            return true;
        else
            return false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

    }


    @Override
    public void Onclick() {

    }
}

