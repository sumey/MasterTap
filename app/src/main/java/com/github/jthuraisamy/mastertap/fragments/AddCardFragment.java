package com.github.jthuraisamy.mastertap.fragments;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.jthuraisamy.mastertap.R;

public class AddCardFragment extends Fragment {

    public static AddCardFragment create() {
        return new AddCardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_card_fragment, container, false);
    }
}
