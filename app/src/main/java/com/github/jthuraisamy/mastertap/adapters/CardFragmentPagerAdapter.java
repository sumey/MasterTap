package com.github.jthuraisamy.mastertap.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.github.jthuraisamy.mastertap.MainActivity;
import com.github.jthuraisamy.mastertap.R;
import com.github.jthuraisamy.mastertap.fragments.AddCardFragment;
import com.github.jthuraisamy.mastertap.fragments.CardFragment;

public class CardFragmentPagerAdapter extends FragmentStatePagerAdapter {
    private final FragmentActivity ctx;

    public CardFragmentPagerAdapter(FragmentActivity ctx) {
        super(ctx.getSupportFragmentManager());

        this.ctx = ctx;
    }

    @Override
    public int getCount() {
        return MainActivity.cards.size();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return AddCardFragment.create();
            default:
                return CardFragment.create(position);
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return ctx.getText(R.string.add_card);
            default:
                return MainActivity.cards.get(position).getLabel();
        }
    }
}
